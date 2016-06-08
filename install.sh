#!/bin/sh
#
# Creates a single install directory from all the csw stage directories.

dir=../install

hash sbt 2>/dev/null || { echo >&2 "Please install sbt first.  Aborting."; exit 1; }

test -d $dir || mkdir -p $dir/bin $dir/lib $dir/conf
sbt stage

for i in bin lib ; do
    for j in */target/universal/stage/$i/* ; do
        cp -f $j $dir/$i
    done
done

rm -f $dir/bin/*.log.* $dir/bin/*.bat

chmod ugo+x scripts/*
cp scripts/* $dir/bin/



