use strict;
use LWP::Simple;


my $ac = 'CY073775.1, CY022055.1, U47817.1';
my $efetch = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?"."db=nucleotide&id=$ac&rettype=gbwithparts&retmode=text";

my $efetch_result = get($efetch);
print "$efetch_result";


