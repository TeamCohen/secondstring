Sample data files for use with the secondstring.expt package.

These files contain tab-delimited text fields such that:
 - field 1 is the name of the relation from which this example comes
 - field 2 is (usually) an ID value, which can be used to assess the correctness of a match
 - the remaining fields are text fields that identify an entity. Usually there's only one
such field. 

==============================================================================

animal.txt
	two relations: park_animals and endangered_species.
	there is no key for these relations - instead, a match is considered
	correct if the tokens in either scientific name are a proper
	subset of the other.
birdKunkel.txt
	two relations: map1 and map2.  keys are URLs
birdNybirdExtracted.txt
	three relations: nabird, callx.  keys are scientific names.
	the "entity names" (birds) were originally embedded in additional text,
	which has been (manually) removed.
birdScott1.txt
	two relations: cscott and dsarkimg.  keys are URLS
birdScott2.txt
	two relations: bscott and mbrimg.  keys are URLS
business.txt
	two relations: hooverweb and iontech.  keys are top-level URLs.
	roughly 10-15% of the URLs don't match when they should.
censusText.txt
	two relations.
	textual fields from synthetic census data.  original source: William Winkler
coraATDV.txt
	one relation.
	cora-test dataset with fields author, title, date, and venue concatenated
gameExtracted.txt
	two relations: demox, and newsweek.  the keys are hand-generated.
	the "entity names" in demo were originally embedded in additional text,
	which has been (manually) removed.
	[thanks to Nick Kushmeric and Misha Bilenko for fixing a bug with
	 this one, Nov '03]
parks.txt
	two relations: npspark and icepark.  keys are URLs, which seem to be
	much less noisy for this case than for the business domain.
restaurant.txt
	two relations: f and z (Fodor's and Zagrat's, respectively).
	Originally from Sheila Tejada at ISI.  Keys are phone numbers,
	manually edited to reflect the few cases in which phone
	numbers are not correct keys.
ucbpeopleMatch.txt
	two relations: ucb1 and ucb2. source: Nick Kushmeric at UC/Dublin.
	original source: Alvaro Monge, I think.
ucbpeopleCluster.txt
	one relation. source: Nick Kushmeric at UC/Dublin. original source: Alvaro Monge, I think.
	ucbpeopleMatch.txt was created by splitting this up by source, when source was known.
vaUniv.txt
	one relation: vauniv. source: Nick Kushmeric at UC/Dublin. 
	original source: Alvaro Monge, I think.
