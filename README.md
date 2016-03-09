GitHub Api client 
===========================

# Consists of 

* Single Akka Actor 
* GithubApi client that uses Spray-can for requests
* Play-json for parsing github json responses

Should be able to connect to gitHubApi and fetch repositories with contributors, evaluate influence based on given formula. 
Has some simple specs for actor behaviour and evaluation util.

Lacks a test for github communication.

Also due to amount of requests public github api will sometimes return 

    `{
       "message": "API rate limit exceeded for xyz (But here's the good news: Authenticated requests get a higher rate limit. Check out the documentation for more details.)",
       "documentation_url": "https://developer.github.com/v3/#rate-limiting"
     }`
                                                                                 
# TODO: 
* Implement some sort of delay's between requests or worker actors, that could handle inner requests


