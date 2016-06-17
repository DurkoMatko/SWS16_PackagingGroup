.PHONY: compile run db

compile:
	mvn clean install

run: compile
	mvn exec:java

db:
	./start-fuseki.sh
