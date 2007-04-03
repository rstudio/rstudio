import sys, os, os.path

def tweak(fn):
    newName = "fixed/" + fn
    f = open(fn, 'r')
    outf = open(newName, 'w')
    seen = []
    print fn
    for line in f:
        cleaned = line.strip()
        if len(cleaned) == 0:
            continue
        if (cleaned.startswith("#")):
            [] # ignore the comment
        elif (seen.count(cleaned) == 0):
            outf.write(cleaned)
            outf.write('\n')
            seen.append(cleaned)
        else:
            [] # skip the duplicate line
    f.close()

for f in os.listdir("."):
    if (not(f.endswith(".properties"))):
        continue
    elif (f.startswith("DateTimeConstants")):
        tweak(f)
    elif (f.startswith("NumberConstants")):
        tweak(f)
    elif (f.startswith("CurrencyCodeMapConstants")):
        tweak(f)

        


