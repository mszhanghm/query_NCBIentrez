import gov.nih.nlm.ncbi.www.soap.eutils.*;
import org.xml.sax.SAXParseException;
import java.io.*;
import java.net.*;

public class Entrez
{
  private static EUtilsServiceStub service;
  private static EUtilsServiceStub.ESearchRequest req;

  public Entrez() throws Exception 
  {
    service = new EUtilsServiceStub();
    req = new EUtilsServiceStub.ESearchRequest();
  }


/*********************************************************************************
 * M E T H O D S
 * -------------
 * 
 * SEARCH:
 * - int          response(String db, String query, String retMax)
 * - void         search(String db, String query, String retMax, String file_name)
 * 
 * MAP:
 * - String       PMID(String PMCID)
 * - String       urlPMC(String PMCID)
 * - String       urlPubMed(String PMID)
 * 
 * FETCH:
 * - Citation     getCitation(String db, String id)
 * - String       getAbstract(String pmid)
 * - String       getPaper(String id)
 * - void         PMC_fetchFullPaper(String pmc_id, String file_name)
 * - void         PMC_fetchPDF(String pmc_id, String file_name)
 * - StringBuffer bufferPMC(String PMCID)
 *
 * PROCESS:
 * - String       cleanPMC_XMLtags(String text)
 * - String       cleanPMCAbstractTags(String text)
 * - String       cleanPMC_HTMLtags(StringBuffer target)
 * - String       MandM(String paper)
 ********************************************************************************/



  // * * * * * * * * * * * * * * *   S E A R C H   * * * * * * * * * * * * * * * *

  // -----------------------------------------------------------------------------
  // --- determine the number of search results returned
  // -----------------------------------------------------------------------------
  public int response(String db, String query, String retMax) throws Exception 
  {
    int total = 0;

    try
    {
      System.out.println("\nEntrez.response(db, query, retMax):");
      System.out.println("      db = " + db);
      System.out.println("   query = " + query);
      System.out.println("  retMax = " + retMax);

      req.setDb(db);
      req.setTerm(query);
      req.setRetMax(retMax);

      EUtilsServiceStub.ESearchResult res = service.run_eSearch(req);

      if (res.getIdList().getId() != null) total = res.getIdList().getId().length;
    }
    catch (Exception e) {System.out.println(e.toString());}

    return total;
  }
  // -----------------------------------------------------------------------------



  // -----------------------------------------------------------------------------
  // --- query PubMed [Central] & write the search results to a file
  // -----------------------------------------------------------------------------
  public void search(String db, String query, String retMax, String file_name)
  {
    try
    {
      System.out.println("\nEntrez.search(db, query, retMax, file_name):");
      System.out.println("      db = " + db);
      System.out.println("   query = " + query);
      System.out.println("  retMax = " + retMax);

      req.setDb(db);
      req.setTerm(query);
      req.setRetMax(retMax);

      System.out.println("  Writing to file: " + file_name + " ...");
      FileWriter file_out = new FileWriter(file_name);

      // --- call NCBI ESearch utility
      EUtilsServiceStub.ESearchResult res = service.run_eSearch(req);

      // --- OUTPUT: IDs of the matching documents
      if (res.getIdList() != null) if (res.getIdList().getId() != null)
      {
        for(int i = 0; i < res.getIdList().getId().length; i++)
          file_out.write(res.getIdList().getId()[i] + "\n");
      }

      file_out.close();
    }
    catch (Exception e) {System.out.println(e.toString());}
  }
  // -----------------------------------------------------------------------------


  // * * * * * * * * * * * * * * * * *   M A P   * * * * * * * * * * * * * * * * *


  // -----------------------------------------------------------------------------
  // --- map PubMed Central ID to PubMed ID
  // -----------------------------------------------------------------------------
  public String PMID(String PMCID)
  {
    System.out.print("Entrez.PMID(" + PMCID + ") = ");

    String PMID = "";

    try
    {
      // --- download XML page ---
      String base = "http://www.ncbi.nlm.nih.gov/sites/entrez?db=pmc&cmd=DetailsSearch&term=";
      String url  = base + PMCID + "[uid]&dopt=XML";
      URL            u = new URL(url);
      InputStream    is  = u.openStream();
      BufferedReader dis = new BufferedReader(new InputStreamReader(is));
      String         s, anchor = "&lt;article-id pub-id-type=&quot;pmid&quot;&gt;";
      int            i, len = anchor.length();

      while ( (s = dis.readLine()) != null )
      {
        if ( (i = s.indexOf(anchor)) >= 0 )
        {
          s = s.substring(i+len);
          i = s.indexOf("&lt;/article-id&gt;");
          PMID = s.substring(0, i);
          System.out.println(PMID);
          break;
        }
      }
    }
    catch (Exception e) {System.out.println(e.toString());}

    return PMID;
  }
  // -----------------------------------------------------------------------------



  // -----------------------------------------------------------------------------
  // --- generate PubMed URL for the article with the given ID
  // -----------------------------------------------------------------------------
  public String urlPubMed(String PMID)
  {
    String url_base = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=";
    return url_base + PMID;
  }
  // -----------------------------------------------------------------------------



  // -----------------------------------------------------------------------------
  // --- generate PubMed Central URL for the article with the given ID
  // -----------------------------------------------------------------------------
  public String urlPMC(String PMCID)
  {
    String url_base = "http://www.pubmedcentral.nih.gov/articlerender.fcgi?tool=pmcentrez&artid=";
    return url_base + PMCID;
  }
  // -----------------------------------------------------------------------------


  // * * * * * * * * * * * * * * * *   F E T C H   * * * * * * * * * * * * * * * *


  // -----------------------------------------------------------------------------
  // --- retrieve citation details for the article with the given ID:
  //     return new Citation(id, authors, title, source, abstractText);
  // -----------------------------------------------------------------------------
  public Citation getCitation(String db, String id)
  {
    System.out.println("\nEntrez.getCitation(" + db + ", " + id + "): fetch citation details");

    String title = "";
    String authors = "";
    String source = "";
    String abstractText = "";

    // --- get PMID for the article
    String pmid = id;
    if (db.toLowerCase().equals("pmc")) pmid = PMID(id);

    try
    {
      // --- download the summary page from PubMed
      String base = "http://www.ncbi.nlm.nih.gov/pubmed/";   // --- PubMed URL
      String url = base + pmid + "?dopt=Summary";            // --- display option: Summary
      URL            u = new URL(url);
      InputStream    is  = u.openStream();
      BufferedReader dis = new BufferedReader(new InputStreamReader(is));
      String         s, download = "";
      while ( (s = dis.readLine()) != null ) download += s;

      // --- extract TITLE ---
      int b = 0, e = 0, offset = 0;
      String anchor = "<p class=\"title\">";
      if ( (b = download.indexOf(anchor)) >= 0)
      {
        b += anchor.length();
        anchor = "</p>";
        e = download.indexOf(anchor, b);
        title = download.substring(b, e);
        title = title.replaceAll("<a[^>]*>", "");
        title = title.replaceAll("</a>", "");
      }

      // --- extract AUTHORS ---
      b = 0; e = 0; offset = 0;
      anchor = "<p class=\"authors\">";
      if ( (b = download.indexOf(anchor)) >= 0)
      {
        b += anchor.length();
        anchor = "</p>";
        e = download.indexOf(anchor, b);
        authors = download.substring(b, e);
      }

      // --- extract SOURCE ---
      b = 0; e = 0; offset = 0;
      anchor = "<p class=\"source\">";
      if ( (b = download.indexOf(anchor)) >= 0)
      {
        b += anchor.length();
        anchor = "</p>";
        e = download.indexOf(anchor, b);
        source = download.substring(b, e);
        source = source.replaceAll("<span[^>]*>", "");
        source = source.replaceAll("</span>", "");
      }

      // --- extract abstract TEXT ---
      abstractText = getAbstract(pmid);
    }
    catch (Exception e) {System.out.println(e.toString());}

    return new Citation(id, authors, title, source, abstractText);
  }
  // -----------------------------------------------------------------------------



  // -----------------------------------------------------------------------------
  // --- get the text of an abstract from PubMed
  // -----------------------------------------------------------------------------
  public String getAbstract(String pmid)
  {
    System.out.println("\nEntrez.getAbstract(" + pmid + ")\n");

    String output = "";

    try
    {
      String base = "http://www.ncbi.nlm.nih.gov/pubmed/";   // --- PubMed URL
      String url  = base + pmid + "?dopt=XML";

      // --- download abstract page
      URL            u = new URL(url);
      InputStream    is  = u.openStream();
      BufferedReader dis = new BufferedReader(new InputStreamReader(is));
      String         s, download = "";
      while ( (s = dis.readLine()) != null ) download += s;

      // --- extract article TITLE ---
      String articleTitle = "";
      int b = 0, e = 0, offset = 0;
      String anchor = "&lt;ArticleTitle&gt;<font class=\"val\">";
      if ( (b = download.indexOf(anchor)) >= 0)
      {
        b += anchor.length();
        anchor = "</font>&lt;/ArticleTitle&gt;";
        e = download.indexOf(anchor);
        articleTitle = download.substring(b, e);
      }

      // --- extract abstract TEXT ---
      String abstractText = "";
      anchor = "&lt;AbstractText&gt;<font class=\"val\">";
      if ( (b = download.indexOf(anchor)) >= 0)
      {
        b += anchor.length();
        anchor = "</font>&lt;/AbstractText&gt;";
        e = download.indexOf(anchor);
        abstractText = download.substring(b, e);
      }

      output = articleTitle + "\n\n" + abstractText;
    }
    catch (Exception e) {System.out.println(e.toString());}

    return output;
  }
  // -----------------------------------------------------------------------------



  // -----------------------------------------------------------------------------
  // --- fetch full-text article from PubMed Central
  // -----------------------------------------------------------------------------
  public String getPaper(String id)
  {
    System.out.println("Entrez.getPaper(" + id + "): fetch paper from PubMed Central");

    String doc = "";

    try
    {
      java.net.URL   u;
      InputStream    ins = null;
      BufferedReader dis;
      String         s;
      u   = new java.net.URL("http://www.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pmc&id=" + id);
      ins = u.openStream();  // --- throws an IOException
      dis = new BufferedReader(new InputStreamReader(ins));
      while ( (s = dis.readLine()) != null ) doc += s;
      if (doc.compareTo("") == 0) doc = null;
    }
    catch (Exception e) {System.out.println(e.toString());}

    return doc;
  }
  // -----------------------------------------------------------------------------



  // -----------------------------------------------------------------------------
  // --- fetch full-text article from PubMed Central & save to file
  // -----------------------------------------------------------------------------
  public void PMC_fetchFullPaper(String pmc_id, String file_name)
  {
    System.out.println("Entrez.PMC_fetchFullPaper(" + pmc_id + ", " + file_name + "): download PMC paper in XML format");

    try
    {
      java.net.URL   u;
      InputStream    is = null;
      BufferedReader dis;
      String         s;
      u  = new java.net.URL("http://www.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pmc&id=" + pmc_id);
      is = u.openStream();
      dis = new BufferedReader(new InputStreamReader(is));
      FileWriter pmc_out = new FileWriter(file_name);
      while ( (s = dis.readLine()) != null ) pmc_out.write(s + "\n");
      pmc_out.close();
      is.close();
    }
    catch (MalformedURLException mue) {mue.printStackTrace();} 
    catch (IOException ioe)           {ioe.printStackTrace();}
    catch (Exception e)               {System.out.println(e.toString());}
  }
  // -----------------------------------------------------------------------------



  // -----------------------------------------------------------------------------
  // --- fetch PDF from PubMed Central & save to file
  // -----------------------------------------------------------------------------
  public void PMC_fetchPDF(String pmc_id, String file_name)
  {
    System.out.println("Entrez.PMC_fetchPDF(" + pmc_id + ", " + file_name + "): download PMC paper in PDF format");

    try
    {
      java.io.BufferedInputStream  bin  = new java.io.BufferedInputStream(new java.net.URL("http://www.pubmedcentral.nih.gov/picrender.fcgi?tool=pmcentrez&artid=" + pmc_id + "&blobtype=pdf").openStream());
      java.io.FileOutputStream     fos  = new java.io.FileOutputStream(file_name);
      java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
      byte data[] = new byte[1];
      while(bin.read(data, 0, 1) >= 0) bout.write(data);
      bout.close(); fos.close(); bin.close();
    }
    catch(EOFException eof) {System.out.println("EOFException: " + eof.getMessage());} 
    catch(IOException  ioe) {System.out.println("IOException: "  + ioe.getMessage());} 
    catch(Exception    e)   {System.out.println("Exception: "    + e.getMessage());  }
  }
  // -----------------------------------------------------------------------------



  // -----------------------------------------------------------------------------
  // --- buffer HTML version of full-text article from PubMed Central
  // -----------------------------------------------------------------------------
  public StringBuffer bufferPMC(String PMCID)
  {
    System.out.println("Entrez.bufferPMC(" + PMCID + "): download PMC paper in HTML format");

    try
    {
      java.net.URL   u;
      InputStream    ins = null;
      BufferedReader dis;
      int            c;
      StringBuffer   doc = new StringBuffer();
      u   = new java.net.URL(urlPMC(PMCID));
      ins = u.openStream();
      dis = new BufferedReader(new InputStreamReader(ins));

      int size = 0;

      while ( (c = dis.read()) >= 0 )
      {
        if (size > 2 * 1024 * 1024) 
        {
          System.out.println("Warning: HTML file is too BIG to download!");
          doc = null;
          Runtime runtime = Runtime.getRuntime();
          runtime.gc();
          return new StringBuffer("Document size exceeding 2MB! Click on the link above to see the document at the PubMed Central.");
        }
        doc.append((char) c);
        size++;
      }

      return doc;
    }
    catch (MalformedURLException mue)
    {
      mue.printStackTrace();
      System.out.println("Warning: Document not downloaded due to an MalformedURLException!");
      return new StringBuffer("Document not downloaded due to an MalformedURLException.");
     } 
    catch (IOException ioe)
    {
      ioe.printStackTrace();
      System.out.println("Warning: Document not downloaded due to an IOException!");
      return new StringBuffer("Document not downloaded due to an IOException.");
    }
    catch (Exception ex)
    {
      System.out.println("Warning: An exception occurred during the download.");
      ex.printStackTrace();
    }

    return new StringBuffer("");
  }
  // -----------------------------------------------------------------------------


  // * * * * * * * * * * * * * * *   P R O C E S S   * * * * * * * * * * * * * * *


  // -----------------------------------------------------------------------------
  // --- clean the text from PubMed Central XML tags
  // -----------------------------------------------------------------------------
  public String cleanPMC_XMLtags(String text)
  {
    String clean = text;

    clean = clean.replaceAll("</abstract>", "");
    clean = clean.replaceAll("<abstract[^>]*>", "");
    clean = clean.replaceAll("</ack>", "");
    clean = clean.replaceAll("<ack[^>]*>", "");
    clean = clean.replaceAll("</addr-line>", "");
    clean = clean.replaceAll("<addr-line[^>]*>", "");
    clean = clean.replaceAll("</aff>", "");
    clean = clean.replaceAll("<aff[^>]*>", "");
    clean = clean.replaceAll("</alt-title>", "");
    clean = clean.replaceAll("<alt-title[^>]*>", "");
    clean = clean.replaceAll("</article>", "");
    clean = clean.replaceAll("<article[^>]*>", "");
    clean = clean.replaceAll("</article-categories>", "");
    clean = clean.replaceAll("<article-categories[^>]*>", "");
    clean = clean.replaceAll("</article-id>", "");
    clean = clean.replaceAll("<article-id[^>]*>", "");
    clean = clean.replaceAll("</article-meta>", "");
    clean = clean.replaceAll("<article-meta[^>]*>", "");
    clean = clean.replaceAll("</article-title>", "");
    clean = clean.replaceAll("<article-title[^>]*>", "");
    clean = clean.replaceAll("</author-notes>", "");
    clean = clean.replaceAll("<author-notes[^>]*>", "");
    clean = clean.replaceAll("</back>", "");
    clean = clean.replaceAll("<back[^>]*>", "");
    clean = clean.replaceAll("</body>", "");
    clean = clean.replaceAll("<body[^>]*>", "");
    clean = clean.replaceAll("</bold>", "");
    clean = clean.replaceAll("<bold[^>]*>", "");
    clean = clean.replaceAll("</caption>", "");
    clean = clean.replaceAll("<caption[^>]*>", "");
    clean = clean.replaceAll("</citation>", "");
    clean = clean.replaceAll("<citation[^>]*>", "");
    clean = clean.replaceAll("</contract-num>", "");
    clean = clean.replaceAll("<contract-num[^>]*>", "");
    clean = clean.replaceAll("</contract-sponsor>", "");
    clean = clean.replaceAll("<contract-sponsor[^>]*>", "");
    clean = clean.replaceAll("</contrib>", "");
    clean = clean.replaceAll("<contrib[^>]*>", "");
    clean = clean.replaceAll("</contrib-group>", "");
    clean = clean.replaceAll("<contrib-group[^>]*>", "");
    clean = clean.replaceAll("</copyright-holder>", "");
    clean = clean.replaceAll("<copyright-holder[^>]*>", "");
    clean = clean.replaceAll("</copyright-statement>", "");
    clean = clean.replaceAll("<copyright-statement[^>]*>", "");
    clean = clean.replaceAll("</copyright-year>", "");
    clean = clean.replaceAll("<copyright-year[^>]*>", "");
    clean = clean.replaceAll("</corresp>", "");
    clean = clean.replaceAll("<corresp[^>]*>", "");
    clean = clean.replaceAll("</counts>", "");
    clean = clean.replaceAll("<counts[^>]*>", "");
    clean = clean.replaceAll("</custom-meta>", "");
    clean = clean.replaceAll("<custom-meta[^>]*>", "");
    clean = clean.replaceAll("</custom-meta-wrap>", "");
    clean = clean.replaceAll("<custom-meta-wrap[^>]*>", "");
    clean = clean.replaceAll("</date>", "");
    clean = clean.replaceAll("<date[^>]*>", "");
    clean = clean.replaceAll("</day>", "");
    clean = clean.replaceAll("<day[^>]*>", "");
    clean = clean.replaceAll("</dc:author>", "");
    clean = clean.replaceAll("<dc:author[^>]*>", "");
    clean = clean.replaceAll("</dc:date>", "");
    clean = clean.replaceAll("<dc:date[^>]*>", "");
    clean = clean.replaceAll("</dc:identifier>", "");
    clean = clean.replaceAll("<dc:identifier[^>]*>", "");
    clean = clean.replaceAll("</dcterms:bibliographicCitation>", "");
    clean = clean.replaceAll("<dcterms:bibliographicCitation[^>]*>", "");
    clean = clean.replaceAll("</dcterms:isPartOf>", "");
    clean = clean.replaceAll("<dcterms:isPartOf[^>]*>", "");
    clean = clean.replaceAll("</dc:title>", "");
    clean = clean.replaceAll("<dc:title[^>]*>", "");
    clean = clean.replaceAll("</dc:type>", "");
    clean = clean.replaceAll("<dc:type[^>]*>", "");
    clean = clean.replaceAll("</degrees>", "");
    clean = clean.replaceAll("<degrees[^>]*>", "");
    clean = clean.replaceAll("</edition>", "");
    clean = clean.replaceAll("<edition[^>]*>", "");
    clean = clean.replaceAll("</email>", "");
    clean = clean.replaceAll("<email[^>]*>", "");
    clean = clean.replaceAll("</ext-link>", "");
    clean = clean.replaceAll("<ext-link[^>]*>", "");
    clean = clean.replaceAll("</fig>", "");
    clean = clean.replaceAll("<fig[^>]*>", "");
    clean = clean.replaceAll("</fig-count>", "");
    clean = clean.replaceAll("<fig-count[^>]*>", "");
    clean = clean.replaceAll("</fn>", "");
    clean = clean.replaceAll("<fn[^>]*>", "");
    clean = clean.replaceAll("</fn-group>", "");
    clean = clean.replaceAll("<fn-group[^>]*>", "");
    clean = clean.replaceAll("</fpage>", "");
    clean = clean.replaceAll("<fpage[^>]*>", "");
    clean = clean.replaceAll("</front>", "");
    clean = clean.replaceAll("<front[^>]*>", "");
    clean = clean.replaceAll("</given-names>", "");
    clean = clean.replaceAll("<given-names[^>]*>", "");
    clean = clean.replaceAll("</graphic>", "");
    clean = clean.replaceAll("<graphic[^>]*>", "");
    clean = clean.replaceAll("</history>", "");
    clean = clean.replaceAll("<history[^>]*>", "");
    clean = clean.replaceAll("</issn>", "");
    clean = clean.replaceAll("<issn[^>]*>", "");
    clean = clean.replaceAll("</issue>", "");
    clean = clean.replaceAll("<issue[^>]*>", "");
    clean = clean.replaceAll("</italic>", "");
    clean = clean.replaceAll("<italic[^>]*>", "");
    clean = clean.replaceAll("</journal-id>", "");
    clean = clean.replaceAll("<journal-id[^>]*>", "");
    clean = clean.replaceAll("</journal-meta>", "");
    clean = clean.replaceAll("<journal-meta[^>]*>", "");
    clean = clean.replaceAll("</journal-title>", "");
    clean = clean.replaceAll("<journal-title[^>]*>", "");
    clean = clean.replaceAll("</kwd>", "");
    clean = clean.replaceAll("<kwd[^>]*>", "");
    clean = clean.replaceAll("</kwd-group>", "");
    clean = clean.replaceAll("<kwd-group[^>]*>", "");
    clean = clean.replaceAll("</label>", "");
    clean = clean.replaceAll("<label[^>]*>", "");
    clean = clean.replaceAll("</license>", "");
    clean = clean.replaceAll("<license[^>]*>", "");
    clean = clean.replaceAll("</License>", "");
    clean = clean.replaceAll("<License[^>]*>", "");
    clean = clean.replaceAll("</lpage>", "");
    clean = clean.replaceAll("<lpage[^>]*>", "");
    clean = clean.replaceAll("</meta-name>", "");
    clean = clean.replaceAll("<meta-name[^>]*>", "");
    clean = clean.replaceAll("</meta-value>", "");
    clean = clean.replaceAll("<meta-value[^>]*>", "");
    clean = clean.replaceAll("</month>", "");
    clean = clean.replaceAll("<month[^>]*>", "");
    clean = clean.replaceAll("</name>", "");
    clean = clean.replaceAll("<name[^>]*>", "");
    clean = clean.replaceAll("</notes>", "");
    clean = clean.replaceAll("<notes[^>]*>", "");
    clean = clean.replaceAll("</p>", "");
    clean = clean.replaceAll("<p[^>]*>", "");
    clean = clean.replaceAll("</page-count>", "");
    clean = clean.replaceAll("<page-count[^>]*>", "");
    clean = clean.replaceAll("</permissions>", "");
    clean = clean.replaceAll("<permissions[^>]*>", "");
    clean = clean.replaceAll("</permits>", "");
    clean = clean.replaceAll("<permits[^>]*>", "");
    clean = clean.replaceAll("</person-group>", "");
    clean = clean.replaceAll("<person-group[^>]*>", "");
    clean = clean.replaceAll("</pmc-articleset>", "");
    clean = clean.replaceAll("<pmc-articleset[^>]*>", "");
    clean = clean.replaceAll("</pub-date>", "");
    clean = clean.replaceAll("<pub-date[^>]*>", "");
    clean = clean.replaceAll("</pub-id>", "");
    clean = clean.replaceAll("<pub-id[^>]*>", "");
    clean = clean.replaceAll("</publisher>", "");
    clean = clean.replaceAll("<publisher[^>]*>", "");
    clean = clean.replaceAll("</publisher-loc>", "");
    clean = clean.replaceAll("<publisher-loc[^>]*>", "");
    clean = clean.replaceAll("</publisher-name>", "");
    clean = clean.replaceAll("<publisher-name[^>]*>", "");
    clean = clean.replaceAll("</ref>", "");
    clean = clean.replaceAll("<ref[^>]*>", "");
    clean = clean.replaceAll("</ref-count>", "");
    clean = clean.replaceAll("<ref-count[^>]*>", "");
    clean = clean.replaceAll("</ref-list>", "");
    clean = clean.replaceAll("<ref-list[^>]*>", "");
    clean = clean.replaceAll("</requires>", "");
    clean = clean.replaceAll("<requires[^>]*>", "");
    clean = clean.replaceAll("</role>", "");
    clean = clean.replaceAll("<role[^>]*>", "");
    clean = clean.replaceAll("</sc>", "");
    clean = clean.replaceAll("<sc[^>]*>", "");
    clean = clean.replaceAll("</sec>", "");
    clean = clean.replaceAll("<sec[^>]*>", "");
    clean = clean.replaceAll("</self-uri>", "");
    clean = clean.replaceAll("<self-uri[^>]*>", "");
    clean = clean.replaceAll("</series-title>", "");
    clean = clean.replaceAll("<series-title[^>]*>", "");
    clean = clean.replaceAll("</source>", "");
    clean = clean.replaceAll("<source[^>]*>", "");
    clean = clean.replaceAll("</sub>", "");
    clean = clean.replaceAll("<sub[^>]*>", "");
    clean = clean.replaceAll("</subject>", "");
    clean = clean.replaceAll("<subject[^>]*>", "");
    clean = clean.replaceAll("</subj-group>", "");
    clean = clean.replaceAll("<subj-group[^>]*>", "");
    clean = clean.replaceAll("</suffix>", "");
    clean = clean.replaceAll("<suffix[^>]*>", "");
    clean = clean.replaceAll("</sup>", "");
    clean = clean.replaceAll("<sup[^>]*>", "");
    clean = clean.replaceAll("</surname>", "");
    clean = clean.replaceAll("<surname[^>]*>", "");
    clean = clean.replaceAll("</table>", "");
    clean = clean.replaceAll("<table[^>]*>", "");
    clean = clean.replaceAll("</table-count>", "");
    clean = clean.replaceAll("<table-count[^>]*>", "");
    clean = clean.replaceAll("</table-wrap>", "");
    clean = clean.replaceAll("<table-wrap[^>]*>", "");
    clean = clean.replaceAll("</table-wrap-foot>", "");
    clean = clean.replaceAll("<table-wrap-foot[^>]*>", "");
    clean = clean.replaceAll("</tbody>", "");
    clean = clean.replaceAll("<tbody[^>]*>", "");
    clean = clean.replaceAll("</td>", "");
    clean = clean.replaceAll("<td[^>]*>", "");
    clean = clean.replaceAll("</thead>", "");
    clean = clean.replaceAll("<thead[^>]*>", "");
    clean = clean.replaceAll("</title>", ". ");
    clean = clean.replaceAll("<title[^>]*>", "");
    clean = clean.replaceAll("</title-group>", "");
    clean = clean.replaceAll("<title-group[^>]*>", "");
    clean = clean.replaceAll("</tr>", "");
    clean = clean.replaceAll("<tr[^>]*>", "");
    clean = clean.replaceAll("</uri>", "");
    clean = clean.replaceAll("<uri[^>]*>", "");
    clean = clean.replaceAll("</volume>", "");
    clean = clean.replaceAll("<volume[^>]*>", "");
    clean = clean.replaceAll("</Work>", "");
    clean = clean.replaceAll("<Work[^>]*>", "");
    clean = clean.replaceAll("</xref>", "");
    clean = clean.replaceAll("<xref[^>]*>", "");
    clean = clean.replaceAll("</year>", "");
    clean = clean.replaceAll("<year[^>]*>", "");
    clean = clean.replaceAll("<inline-graphic[^>]*>", "");
    clean = clean.replaceAll("&#x[^;]*;", "");

    return clean;
  }
  // -----------------------------------------------------------------------------



  // -----------------------------------------------------------------------------
  // --- clean the abstract from PubMed Central XML tags
  // -----------------------------------------------------------------------------
  public String cleanPMCAbstractTags(String text)
  {
    String clean = text;

    // --- remove HTML tags
    clean = clean.replaceAll("</abstract>", "");
    clean = clean.replaceAll("<abstract[^>]*>", "");
    clean = clean.replaceAll("</p>", "");
    clean = clean.replaceAll("<p>", "");
    clean = clean.replaceAll("</sec>", "");
    clean = clean.replaceAll("<sec[^>]*>", "");
    clean = clean.replaceAll("</ext-link>", "");
    clean = clean.replaceAll("<ext-link[^>]*>", "");

    // --- remove HTML elements
    clean = clean.replaceAll("<fig[^>]*>[^<]*</fig>", "");
    clean = clean.replaceAll("<label[^>]*>[^<]*</label>", "");
    clean = clean.replaceAll("<graphic[^>]*>", "");
    clean = clean.replaceAll("<title>Images</title>", "");

    // --- translate HTML tags
    clean = clean.replaceAll("<italic>", "<em>");
    clean = clean.replaceAll("</italic>", "</em>");
    clean = clean.replaceAll("<bold>", "<strong>");
    clean = clean.replaceAll("</bold>", "</strong>");
    clean = clean.replaceAll("<title>", " <strong>");
    clean = clean.replaceAll("</title>", "</strong>. ");

    return clean;
  }
  // -----------------------------------------------------------------------------



  // -----------------------------------------------------------------------------
  // --- clean the text from PubMed Central HTML tags
  // -----------------------------------------------------------------------------
  public String cleanPMC_HTMLtags(StringBuffer target)
  {
    int   copyFrom,   copyTo;
    int deleteFrom, deleteTo;

    copyFrom = target.indexOf("class=\"content-cell\"");
    while (target.charAt(copyFrom) != '<') copyFrom--;
    StringBuffer input = new StringBuffer();
    input.append(target.substring(copyFrom));

    StringBuffer buffer = new StringBuffer();

    // --- remove sidebar cells
    copyFrom = 0;
    copyTo   = input.indexOf("class=\"sidebar-cell\"");

    while (copyTo >= 0)
    {
      while (input.charAt(copyTo) != '<') copyTo--;
      buffer.append(input.substring(copyFrom, copyTo));
      copyFrom = input.indexOf("</td>", copyTo) + 5;
      copyTo   = input.indexOf("class=\"sidebar-cell\"", copyFrom);
    }

    copyTo = input.length();

    buffer.append(input.substring(copyFrom, copyTo));


    // --- remove comments
    input = buffer;
    buffer = new StringBuffer();

    copyFrom = 0;
    copyTo   = input.indexOf("<!--");

    while (copyTo >= 0)
    {
      buffer.append(input.substring(copyFrom, copyTo));
      copyFrom = input.indexOf("-->", copyTo) + 3;
      copyTo   = input.indexOf("<!--", copyFrom);
    }

    copyTo = input.length();

    buffer.append(input.substring(copyFrom, copyTo));


    // --- remove scripts
    input = buffer;
    buffer = new StringBuffer();

    copyFrom = 0;
    copyTo   = input.indexOf("<script");

    while (copyTo >= 0)
    {
      buffer.append(input.substring(copyFrom, copyTo));
      copyFrom = input.indexOf("</script>", copyTo) + 9;
      copyTo   = input.indexOf("<script", copyFrom);
    }

    copyTo = input.length();

    buffer.append(input.substring(copyFrom, copyTo));


    // --- remove footer
    deleteFrom = buffer.indexOf("<div class=\"footer-section\">");
    deleteTo   = buffer.indexOf("</td></tr></table></body></html>");
    buffer.delete(deleteFrom, deleteTo);

    deleteFrom = buffer.indexOf("</table></body></html>");
    deleteTo   = buffer.indexOf("</body></html>");
    buffer.delete(deleteFrom, deleteTo);

    // --- delete tables
    input = buffer;
    buffer = new StringBuffer();

    copyFrom = 0;
    copyTo   = input.indexOf("<table");

    while (copyTo >= 0)
    {
      buffer.append(input.substring(copyFrom, copyTo));
      copyFrom = input.indexOf("</table>", copyTo) + 8;
      copyTo   = input.indexOf("<table", copyFrom);
    }

    copyTo = input.length();

    buffer.append(input.substring(copyFrom, copyTo));


    String clean = buffer.toString();

    clean = clean.replaceAll("[\\xC2\\xA0]", "");
    clean = clean.replaceAll("<img src=\"corehtml/pmc/pmcgifs/rt-arrow.gif\" alt=\"Small right arrow pointing to:\" style=\"vertical-align: middle;\">", "");
    clean = clean.replaceAll("<a[^>]*><img[^>]*></a>", "");
    clean = clean.replaceAll("</td>", "");
    clean = clean.replaceAll("<td[^>]*>", "");
    clean = clean.replaceAll("</tr>", "");
    clean = clean.replaceAll("<tr[^>]*>", "");
    clean = clean.replaceAll("<br>", "");
    clean = clean.replaceAll(" class=\"cite-reflink\"", "");
    clean = clean.replaceAll("<span[^>]*>", "");
    clean = clean.replaceAll("</span>", "");
    clean = clean.replaceAll("<ul[^>]*>", "<ul>");

    clean = clean.replaceAll("<div class=\"back-matter-section\">", "");

    clean = clean.replaceAll("<div class=\"ref-cit-blk\"", "<a");
    clean = clean.replaceAll("<div class=\"ref-label\">", "</a>");
    clean = clean.replaceAll("</div><div class=\"ref-cit\">", " ");
    clean = clean.replaceAll("articlerender.fcgi",   "http://www.pubmedcentral.nih.gov/articlerender.fcgi");
    clean = clean.replaceAll("pagerender.fcgi",      "http://www.pubmedcentral.nih.gov/pagerender.fcgi");
    clean = clean.replaceAll("picrender.fcgi",       "http://www.pubmedcentral.nih.gov/picrender.fcgi");
    clean = clean.replaceAll("tocrender.fcgi",       "http://www.pubmedcentral.nih.gov/tocrender.fcgi");
    clean = clean.replaceAll("about/copyright.html", "http://www.pubmedcentral.nih.gov/about/copyright.html");

    String id = "";
    String artid = "articlerender.fcgi?artid=";
    int i = clean.indexOf(artid);
    if (i >=0 )
    {
      i+= artid.length();
      char digit = clean.charAt(i);
      while ('0' <= digit && digit <= '9')
      {
        id +=digit;
        digit = clean.charAt(++i);
      }
    }
    clean = clean.replaceAll("<a href=\"#E", "<a href=\"http://www.pubmedcentral.nih.gov/articlerender.fcgi?artid=" + id + "#E");
    clean = clean.replaceAll("<a id=\"F[^>]*></a>", "");
    clean = clean.replaceAll("<a id=\"T[^>]*></a>", "");
    clean = clean.replaceAll("class=\"fig-table-link\" [^>]*>", ">");
    clean = clean.replaceAll("<div style=\"clear:both;\"></div>", "");
    clean = clean.replaceAll("<div class=\"section-content\">", "");
    clean = clean.replaceAll("<div class=\"head[^>]*>", "");
    clean = clean.replaceAll("<div style=\"margin[^>]*>", "");
    clean = clean.replaceAll("<div style=\"border[^>]*>", "");
    clean = clean.replaceAll("\\[<a class=\"ref-extlink\"[^>]*>PubMed</a>\\]", "");
    clean = clean.replaceAll("<div[^>]*>", "\n\n");
    clean = clean.replaceAll("</div></div>", "</div>");
    clean = clean.replaceAll("</div>", "\n<br></br>\n");
    clean = clean.replaceAll( "<p>", "\n<br></br>\n");
    clean = clean.replaceAll("</p>", "\n<br></br>\n");

    clean = clean.replaceAll("<body[^>]*>", "");
    clean = clean.replaceAll("</body>", "");
    clean = clean.replaceAll("<!DOCTYPE[^>]*>", "");
    clean = clean.replaceAll("</html>", "");
    clean = clean.replaceAll("<html>", "");

    clean = clean.replaceAll("</br>\\s*<br>", "");
    clean = clean.replaceAll("</br>\\s*<br>", "");
    clean = clean.replaceAll("<br></br>\\s*<a href=\"http://www.pubmedcentral.nih.gov/pagerender.fcgi", "\n<a href=\"http://www.pubmedcentral.nih.gov/pagerender.fcgi");
    clean = clean.replaceAll("or click on a page below to browse page by page.", "or click on a page below to browse page by page.<br></br>");

    return clean;
  }
  // -----------------------------------------------------------------------------



  // -----------------------------------------------------------------------------
  // --- extract the "Materials & Methods" section from the full-text article
  // -----------------------------------------------------------------------------
  public String MandM(String paper)
  {
    String section = "";

    int b, e = 0;

    while ((b = paper.indexOf("<sec sec-type", e)) >= 0)
    {
      int type_b = paper.indexOf('"', b);
      int type_e = paper.indexOf('"', ++type_b);
      String sec_type = paper.substring(type_b, type_e);

      e = b+1;

      if (sec_type.compareTo("methods")           == 0 || 
          sec_type.compareTo("materials")         == 0 || 
          sec_type.compareTo("materials|methods") == 0 || 
          sec_type.compareTo("methods|materials") == 0)
      {
        int stack = 1;

        while (stack > 0)
        {
          int e1 = paper.indexOf("<sec",   e);
          int e2 = paper.indexOf("</sec>", e);

          if (e1 > 0 && e1 < e2) {stack++; e = e1 + 1;}
          else                   {stack--; e = e2 + 6;}
        }

        section = section + paper.substring(b, e);
      }
    }

    return section;
  }
  // -----------------------------------------------------------------------------

}
