curl --location 'http://localhost:8080/receipe' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=69137098F5B87E57F35452B58B5AE49E' \
--data-raw '{
  "ingredientsList": [
    "calabresa",
    "linguiça",
    "arroz",
     "manteiga",
     "alho"                                  
  ],
  "email": "gustavogads4@gmail.com"
}'
