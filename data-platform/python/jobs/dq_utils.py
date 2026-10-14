from __future__ import annotations

from datetime import datetime
from typing import Any

ALLOWED_ENTITY_TYPES = {
    "customer", "product", "inventory", "cart", "order", "payment", "shipment", "refund"
}
REQUIRED_FIELDS = {
    "eventId", "eventType", "entityType", "entityId", "occurredAt", "schemaVersion", "source", "payload"
}

def validate_event(event: dict[str, Any]) -> tuple[bool, str | None]:
    missing = sorted(k for k in REQUIRED_FIELDS if k not in event or event[k] is None)
    if missing:
        return False, f"missing_fields:{','.join(missing)}"
    if event["entityType"] not in ALLOWED_ENTITY_TYPES:
        return False, "unknown_entity_type"
    if not isinstance(event["payload"], dict):
        return False, "payload_not_object"
    try:
        if int(event["schemaVersion"]) < 1:
            return False, "invalid_schema_version"
    except (TypeError, ValueError):
        return False, "invalid_schema_version"
    try:
        datetime.fromisoformat(str(event["occurredAt"]).replace("Z", "+00:00"))
    except ValueError:
        return False, "invalid_occurred_at"
    return True, None
