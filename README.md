# Play-Scala-API-v0
Building a REST API with Play in scala



#Commande linux pour POST et DELETE

curl     --header "Content-type: application/json"     --request POST     --data '{"id":98, "name":"Salade"}'     http://localhost:9000/foods
curl     --header "Content-type: application/json"     --request DELETE    http://localhost:9000/food/2
curl -X PUT -H "Content-Type: application/json" -d '{"id": 10 , "name" : "spaghetti"}' "http://localhost:9000/food/3"
