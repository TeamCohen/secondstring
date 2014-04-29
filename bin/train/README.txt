Data files for use with com.wcohen.ss.AbbreviationAlignment and com.wcohen.ss.expt.ExtractAbbreviations

==============================================================================

abbvAlign_corpus.txt
	Corpus of biomedical documents for training/testing the AbbreviationAlignment distance metric. The corpus format is one line per file.

abbvAlign_gold.txt
	Gold data of the abbreviation pairs from the sample corpus, 'abbvAlign_corpus.txt'.

abbvAlign_strings.txt
	Abbreviation pairs extracted from the sample corpus, 'abbvAlign_corpus.txt', and labeled as either 'short' (for short-form strings) and 'long' (for long-form strings). This can be used for a matching experiment using the AbbreviationAlignment distance metric and an AbbreviationsBlocker. The labeling (the IDs given to correct pairs of <short-form, long-form> abbreviation pairs) in based on the gold data, 'abbvAlign_gold.txt'.

hmmModelParams.txt
	Pre-calculated model parameters for the HMM alignment model used by AbbreviationAlignment, which were calculated by training the model on the sample corpus, 'abbvAlign_corpus.txt'.
