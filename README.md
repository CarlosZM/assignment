# Assignment backend quick start

It assumes the backend is reachable at `http://localhost:8085` and proposes these routes until they are confirmed in the controller:

- `POST /orders` — create one order from JSON
- `GET /orders` — list orders
- `POST /orders/bulk` — create orders from a already existing file in hdfs

## Requirements

- Docker engine available with enough space to run services
- PowerShell or Shell for running some commands

## Build and start

Run these commands from the directory containing the Compose file:

```powershell
docker compose up
```

Follow the logs, first is created the image for backend, then hdfs configuration is started(namenode and datanode), after that spark configuration is started(master and worker).
The las t container to be ready is backend because needs that spark and hdfs is ready to use to make a good coonection

### List of Services available

| Service | Container port | Published on Windows | Purpose | Address inside `lakehouse-net` |
|---|---:|---|---|---|
| backend | 8085 | `localhost:8085` | Spring Boot API | `http://backend:8085` |
| namenode | 9870 | `localhost:9870` | HDFS web UI | `http://namenode:9870` |
| spark-master | 8080 | `localhost:8080` | Spark master UI | `http://spark-master:8080` |
| spark-worker | 8081 | `localhost:8081` | Spark worker UI | `http://spark-worker:8081` |


### Small Diagram 

````mermaid
flowchart LR
    Client["Cliente / curl"]

    subgraph net["Docker Compose · red lakehouse-net"]
        Backend["backend<br/>Spring Boot + Spark driver<br/>API: 8085<br/>Driver RPC: 50501<br/>Block Manager: 50502"]
        Master["spark-master<br/>Spark Standalone Master<br/>RPC: 7077 · UI: 8080"]
        Worker["spark-worker<br/>Spark executor<br/>UI: 8081"]
        NN["namenode<br/>HDFS metadata<br/>RPC: 9000 · UI: 9870"]
        DN["datanode<br/>HDFS data blocks<br/>Transfer: 9866 · UI: 9864"]
    end

    Client -->|"HTTP :8085"| Backend
    Backend -->|"Envía job · Spark RPC :7077"| Master
    Master -->|"Asigna recursos y executors"| Worker
    Worker -->|"Registro y heartbeat"| Master
    Worker -->|"Consulta metadata HDFS :9000"| NN
    Worker -->|"Lee/escribe bloques :9866"| DN
    DN <-->|"Registro, heartbeat y comandos"| NN
    Worker -->|"Callback al driver :50501/:50502"| Backend
```


````

## Load the CSV into HDFS

You have to execute a curl to require access to write files inside hdfs execute the following **curl**. There I'm assuming that you don't have namenode host name available so **localhost** is a valid one and also, file name that for this time is **order-v1.csv**

````shell
curl -i -X PUT "http://localhost:9870/webhdfs/v1/bulk/order/order-v1.csv?op=CREATE&user.name=root&overwrite=true"
````
On the response you will have something similar to the following **http response** you only need Location header to reuse, just in case you don't have hostname availabe in your machine you can change datanode to localhost
```http request
HTTP/1.1 307 Temporary Redirect
Date: Wed, 18 Nov 2026 18:17:28 GMT
Cache-Control: no-cache
Expires: Wed, 18 Nov 2026 18:17:28 GMT
Date: Wed, 18 Nov 2026 18:17:28 GMT
Pragma: no-cache
X-Content-Type-Options: nosniff
X-FRAME-OPTIONS: SAMEORIGIN
X-XSS-Protection: 1; mode=block
Set-Cookie: hadoop.auth="u=root&p=root&t=simple&e=1795061848720&s=VGn1hZbJOId0C7iRFBw+vT/hoOMg8tUYLH2w53Wo+n8="; Path=/; HttpOnly
Location: http://datanode:9864/webhdfs/v1/bulk/order/order-v1.csv?op=CREATE&user.name=root&namenoderpcaddress=namenode:9000&createflag=&createparent=true&overwrite=true
Content-Type: application/octet-stream
Content-Length: 0
```

As a second step you have to execute the following curl, again I'm assuming that datanode is not available so you can use localhost and also that we are going to use repository orders.csv to be uploaded
````shell
curl -i -X PUT "http://localhost:9870/webhdfs/v1/bulk/order/order-v1.csv?op=CREATE&user.name=root&overwrite=true"
````

On the response you will have the following message indicating that file is available
```http request
HTTP/1.1 201 Created
location: hdfs://namenode:9000/bulk/order/order-v1.csv
content-length: 0
access-control-allow-origin: *
connection: close

```

To test that everything is correct acces to http://localhost:9870/explorer.html#/bulk/order

## Operations with spark + iceberg

To list orders we have the following curl by default returns an empty array

#### REQUEST
```shell
curl --location 'localhost:8085/orders' 
````

#### RESPONSE
```json
[]
```

To create an specific order we have the following curl by default returns a json object

#### REQUEST
```shell
curl --location 'localhost:8085/orders' \
--header 'Content-Type: application/json' \
--data '{
    "order_id":13,
    "customer_id": "C02",
    "product": "Piscos",
    "category": "Licores",
    "quantity": 3,
    "unit_price": 4.4,
    "order_date": "2026-11-02",
    "country": "MX"
}'
````

#### RESPONSE
```json
{
  "order_id": 13,
  "customer_id": "C02",
  "product": "Piscos",
  "category": "Licores",
  "quantity": 3,
  "unit_price": 4.4,
  "order_date": "2026-11-02",
  "country": "MX"
}
```

To perform bulk operation you have the following curl assuming that file is already uploaded, the result means that only those records are being inserted. Validations like null, bad format date, bad format number and duplications are done inside the service

#### REQUEST
```shell
curl --location 'localhost:8085/orders/bulk/order-v1.csv' 
````

#### RESPONSE
```json
[
  {
    "order_id": 1001,
    "customer_id": "C001",
    "product": "Laptop Stand",
    "category": "Accessories",
    "quantity": 2,
    "unit_price": 29.99,
    "order_date": "2026-01-05",
    "country": "CO"
  },
  {
    "order_id": 1002,
    "customer_id": "C002",
    "product": "Mechanical Keyboard",
    "category": "Peripherals",
    "quantity": 1,
    "unit_price": 89.5,
    "order_date": "2026-01-05",
    "country": "MX"
  },
  {
    "order_id": 1003,
    "customer_id": "C003",
    "product": "USB-C Hub",
    "category": "Accessories",
    "quantity": 3,
    "unit_price": 45.0,
    "order_date": "2026-01-06",
    "country": "PE"
  },
  {
    "order_id": 1004,
    "customer_id": "C001",
    "product": "Monitor 27in",
    "category": "Displays",
    "quantity": 1,
    "unit_price": 319.99,
    "order_date": "2026-01-07",
    "country": "CO"
  },
  {
    "order_id": 1005,
    "customer_id": "C004",
    "product": "Webcam HD",
    "category": "Peripherals",
    "quantity": 2,
    "unit_price": 59.9,
    "order_date": "2026-01-07",
    "country": "MX"
  },
  {
    "order_id": 1006,
    "customer_id": "C005",
    "product": "Laptop Stand",
    "category": "Accessories",
    "quantity": 1,
    "unit_price": 29.99,
    "order_date": "2026-01-08",
    "country": "CL"
  },
  {
    "order_id": 1007,
    "customer_id": "C002",
    "product": "Wireless Mouse",
    "category": "Peripherals",
    "quantity": 4,
    "unit_price": 24.5,
    "order_date": "2026-01-09",
    "country": "MX"
  }
]
```

## Design notes

- The partition field used was order_date due to this field will be relevant when performing queries over orders like fetching date ranges or some others. Other two fields that are candidates for partition are country and category 
- Bad rows are validated with a not null validation filtering and also with a wrong format is relied in java code side, a future work should be to move that logic into spark management currently is managed by java application
- Duplicates are handled by java code asumming all object is equal when bulk upload is made and in order to prevent duplication when insert a small query to ask for id's is done 
- For production:
  - all usage of stream api should change to dataframe api
  - better management of dependencies, maybe a try to spark-connect
  - improve serialization strategy from dataset -> java list and from java list -> dataset
  - add validation for edge cases like what to do when spark is down or hadoop is down