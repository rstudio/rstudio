FROM python:3.13.0b4-bookworm

COPY requirements.txt /tmp/requirements.txt

RUN pip3 install -r /tmp/requirements.txt

COPY run-detect-secrets /app/run-detect-secrets

ENTRYPOINT [ "/app/run-detect-secrets" ]
