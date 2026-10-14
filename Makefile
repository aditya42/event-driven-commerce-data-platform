SHELL := /bin/bash

.PHONY: up down logs demo smoke curate reconcile test clean connect

up:
	docker compose up -d --build postgres kafka localstack order-service stream-ingestion

connect:
	docker compose --profile connect up -d --build kafka-connect

logs:
	docker compose logs -f --tail=200 order-service stream-ingestion

demo:
	./scripts/demo.sh

smoke:
	./scripts/smoke-test.sh

curate:
	docker compose --profile jobs run --rm spark-job python /app/jobs/curate_events.py

reconcile:
	docker compose --profile jobs run --rm spark-job python /app/jobs/reconcile_orders.py

test:
	docker compose build --no-cache order-service stream-ingestion
	python3 -m unittest discover -s data-platform/python/tests -p 'test_*.py'

down:
	docker compose down -v --remove-orphans

clean: down
	rm -rf reports data-lake .tmp
