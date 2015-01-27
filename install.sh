#!/bin/sh
#
# Creates a single install directory from all the csw stage directories.

dir=../install

test -d $dir || mkdir -p $dir/{bin,lib,conf}
for i in hardwareopc hcd2opc container2opc ; do (cd $i; sbt publish-local stage); done
for i in bin lib ; do cp -f */target/universal/stage/$i/* $dir/$i/; done
rm -f $dir/bin/*.log.* $dir/bin/*.bat

chmod ugo+x scripts/*
cp scripts/* $dir/bin/

