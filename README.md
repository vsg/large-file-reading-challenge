# Large File Reading Challenge

Solution for the [Large File Reading Challenge](https://github.com/Kyotu-Technology/kyotu/tree/main/recruitment-challenges/large-file-reading-challenge) by Kyotu Technology.

The application exposes a REST endpoint that returns yearly average temperatures for a given city, parsed from a CSV file that can be 3GB+.
The source file may change while the application is running, and the endpoint should reflect the current data.

A slightly faster but more overengineered version, featuring layer decomposition, can be found on the [main branch](https://github.com/vsg/large-file-reading-challenge/tree/main).

## Build

To build the app:

```bash
./mvnw clean package
```

## Run

To run the app:

```bash
./mvnw spring-boot:run
```

By default, the app reads data from `data/example_file.csv`. To use a different data file:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--temperature.data.file=/path/to/file.csv"
```

## Endpoint

```bash
curl "http://localhost:8080/temperature/{city}"
```

## Notes

- A 3GB data file is loaded in about 2 seconds.
- The file is parsed using parallel processing of memory-mapped file blocks.
- Temperature values are parsed using a custom double parser to avoid the overhead of `Double.parseDouble()`.
