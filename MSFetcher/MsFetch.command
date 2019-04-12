#!/bin/bash

##Set folderTag For Region
folderTag=US

##Specified File Name for source file Data, like us_all.csv, use: us_all
sourceFileName=US_ALL

##set if Upolad to GoogleSheet( it will clean anything and write data in that sheet)
uploadGoogleSheet=true

##set if use only single thread, for stock count < 10, use singleThread will much better
singleThread=false


cd $(dirname "$0")
java -jar ./MsFetch.jar "$folderTag" "$sourceFileName" "$uploadGoogleSheet" "$singleThread"