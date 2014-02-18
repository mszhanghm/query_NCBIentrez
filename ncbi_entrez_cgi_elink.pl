use strict;
use LWP::Simple;

#Download protein records linked to a set of nucleotide records corresponding to a list of GI numbers.

my $dbfrom = "nuccore";   #linking from
my $db = "protein"; #linking to
my $linkedname = "nuccore_protein"; # desired link name

# input UIDs in $dbfrom (protein GI numbers)

my $id_list = "306412420, 306412402, 306412365";

# elink URL

my $base = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
my $url = $base."elink.fcgi?dbfrom=$dbfrom&db=$db&id=$id_list";
   $url .= "&linkname=$linkedname&cmd=neighbor_history";
my $elink_result = get($url);

# parse WebEnv and QueryKey
my $web = $1 if ($elink_result =~ /<WebEnv>(\S+)<\/WebEnv>/);
my $key = $1 if ($elink_result =~ /<QueryKey>(\d+)<\/QueryKey>/);

# assemble the efetch URL
 $url = $base."efetch.fcgi?db=$db&query_key=$key&WebEnv=$web";
 $url .= "&rettype=fasta&retmode=text";

my $data = get($url);
print "$data\n";


