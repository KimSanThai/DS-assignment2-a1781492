curl -X POST http://localhost:8000 -v
printf "\n"
curl -X PUT http://localhost:8000 -v -H "Lamport-Clock: 0" -d ""
printf "\n"
curl -X PUT http://localhost:8000 -v -H "Lamport-Clock: 0" -d "?"