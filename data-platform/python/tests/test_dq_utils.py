import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "jobs"))
from dq_utils import validate_event

class ValidationTest(unittest.TestCase):
    def setUp(self):
        self.event = {
            "eventId": "00000000-0000-0000-0000-000000000123",
            "eventType": "ORDER_CREATED",
            "entityType": "order",
            "entityId": "ord-1",
            "occurredAt": "2026-10-14T12:00:00Z",
            "schemaVersion": 1,
            "source": "test",
            "payload": {},
        }

    def test_valid_event(self):
        self.assertEqual((True, None), validate_event(self.event))

    def test_rejects_unknown_domain(self):
        self.event["entityType"] = "unknown"
        self.assertEqual((False, "unknown_entity_type"), validate_event(self.event))

    def test_rejects_bad_timestamp(self):
        self.event["occurredAt"] = "yesterday"
        self.assertEqual((False, "invalid_occurred_at"), validate_event(self.event))

if __name__ == "__main__":
    unittest.main()
