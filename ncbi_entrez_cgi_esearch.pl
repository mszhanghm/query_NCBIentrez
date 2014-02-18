## use the eutils CGI to search entrez databases and fetch the results

use strict;
use LWP::Simple;

my $db = "nucleotide";
my $query = "h1n1 \"segment 6\" influenza a virus";
my $report = "fasta";
my $retmax = "10";


my $Base_URL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
my $esearch = "$Base_URL/esearch.fcgi?"."db=$db&retmax=1&usehistory=y&term=$query";
my $esearch_result = get($esearch);
#print "$esearch_result\n";

$esearch_result =~ m|<Count>(\d+)</Count>.*<QueryKey>(\d+)</QueryKey>.*<WebEnv>(\S+)</WebEnv>|;

my $Count = $1; #print "$Count\n";
my $QueryKey = $2; #print "$QueryKey\n";
my $WebEnv = $3;  #print "$WebEnv\n";

my $efetch = "$Base_URL/efetch.fcgi?"."db=$db&rettype=$report&retmax=$retmax"."&query_key=$QueryKey&WebEnv=$WebEnv";
my $efetch_result = get($efetch);
print "$efetch_result\n";

