rm JSON/Data.txt
clear
java ContentServer localhost:8000 JSON/Test1.txt &
java -cp ".:json-20230618.jar" AggregationServer 8000