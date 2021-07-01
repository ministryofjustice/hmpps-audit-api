    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: OAUTH_ENDPOINT_URL
    value: "{{ .Values.env.OAUTH_ENDPOINT_URL }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "logstash"

  - name: APPINSIGHTS_INSTRUMENTATIONKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

  - name: APPLICATIONINSIGHTS_CONNECTION_STRING
    value: "InstrumentationKey=$(APPINSIGHTS_INSTRUMENTATIONKEY)"

  - name: APPLICATIONINSIGHTS_CONFIGURATION_FILE
    value: "{{ .Values.env.APPLICATIONINSIGHTS_CONFIGURATION_FILE }}"

  - name: HMPPS_SQS_QUEUES_AUDITQUEUE_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: sqs-audit-queue-secret
        key: access_key_id

  - name: HMPPS_SQS_QUEUES_AUDITQUEUE_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: sqs-audit-queue-secret
        key: secret_access_key

  - name: HMPPS_SQS_QUEUES_AUDITQUEUE_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: sqs-audit-queue-secret
        key: sqs_queue_name

  - name: HMPPS_SQS_QUEUES_AUDITQUEUE_DLQ_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: sqs-audit-queue-dl-secret
        key: access_key_id

  - name: HMPPS_SQS_QUEUES_AUDITQUEUE_DLQ_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: sqs-audit-queue-dl-secret
        key: secret_access_key

  - name: HMPPS_SQS_QUEUES_AUDITQUEUE_DLQ_NAME
    valueFrom:
      secretKeyRef:
        name: sqs-audit-queue-dl-secret
        key: sqs_queue_name

  - name: SPRING_DATASOURCE_USERNAME
    valueFrom:
      secretKeyRef:
        name: hmpps-audit-rds-secret
        key: database_username

  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: hmpps-audit-rds-secret
        key: database_password

  - name: DATABASE_NAME
    valueFrom:
      secretKeyRef:
        name: hmpps-audit-rds-secret
        key: database_name

  - name: DATABASE_ENDPOINT
    valueFrom:
      secretKeyRef:
        name: hmpps-audit-rds-secret
        key: rds_instance_endpoint

{{- end -}}
