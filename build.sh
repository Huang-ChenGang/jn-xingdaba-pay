#!/usr/bin/env bash

gradle clean -x test build

docker build --no-cache -t xingdaba/xingdaba-pay .

docker tag xingdaba/xingdaba-pay hub.c.163.com/riyuexingchenace/xingdaba/xingdaba-pay:latest

docker push hub.c.163.com/riyuexingchenace/xingdaba/xingdaba-pay:latest
