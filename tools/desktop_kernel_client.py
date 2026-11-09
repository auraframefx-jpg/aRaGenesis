"""
A.U.R.A.K.A.I. — Desktop Kernel IPC Client & Adapter (Phase 3 Foundation)

Provides Python orchestrators with structured, type-safe IPC communication
with the Kotlin aRaGenesis kernel (kernel-core & language-core).

Adheres strictly to the Epistemic Invariant:
The Python orchestrator is a caller and observation supplier.
It cannot grant itself execution authority; authority is strictly
minted by the Kotlin ExecutionAdmissionGate as an AuthorizedActionToken.
"""

import json
import logging
from dataclasses import dataclass, asdict
from enum import Enum
from typing import Dict, Any, List, Optional

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("DesktopKernelClient")


class RefusalReason(Enum):
    THERMAL_WALL_BREACH = "THERMAL_WALL_BREACH"
    QUARANTINE_BYPASS = "QUARANTINE_BYPASS"
    REPLAY_ATTACK_REJECTION = "REPLAY_ATTACK_REJECTION"
    STALE_EPOCH = "STALE_EPOCH"
    INVALID_SNAPSHOT_BINDING = "INVALID_SNAPSHOT_BINDING"
    UNAUTHORIZED_RECEIPT_CONSTRUCTION = "UNAUTHORIZED_RECEIPT_CONSTRUCTION"
    UNKNOWN = "UNKNOWN"


@dataclass(frozen=True)
class SnapshotBinding:
    blueprint_digest: str
    observation_digests: List[str]
    falsification_trace_digest: str
    snapshot_digest: str


@dataclass(frozen=True)
class TriggerProposal:
    action_type: str
    payload_digest: str
    epoch: int
    nonce_value: str
    snapshot_binding: SnapshotBinding


@dataclass(frozen=True)
class ConsumptionRecord:
    receipt_id: str
    nonce_value: str
    epoch: int
    sequence_number: int


@dataclass(frozen=True)
class AuthorizedActionToken:
    action_type: str
    payload_digest: str
    consumption_record: ConsumptionRecord


@dataclass(frozen=True)
class AdmissionDecision:
    authorized: bool
    token: Optional[AuthorizedActionToken] = None
    refusal_reason: Optional[RefusalReason] = None
    error_message: Optional[str] = None


class DesktopKernelClient:
    """
    Client for interacting with the aRaGenesis Kotlin runtime.
    Validates message envelopes and handles structured proposal submissions.
    """

    def __init__(self, host: str = "127.0.0.1", port: int = 8990):
        self.host = host
        self.port = port
        self.protocol_version = "1.0.0"

    def build_proposal_payload(self, proposal: TriggerProposal) -> Dict[str, Any]:
        """Serializes a TriggerProposal into the canonical IPC format."""
        return {
            "protocolVersion": self.protocol_version,
            "messageType": "PROPOSE_ACTION",
            "payload": {
                "actionType": proposal.action_type,
                "payloadDigest": proposal.payload_digest,
                "epoch": proposal.epoch,
                "nonceValue": proposal.nonce_value,
                "snapshotBinding": {
                    "blueprintDigest": proposal.snapshot_binding.blueprint_digest,
                    "observationDigests": proposal.snapshot_binding.observation_digests,
                    "falsificationTraceDigest": proposal.snapshot_binding.falsification_trace_digest,
                    "snapshotDigest": proposal.snapshot_binding.snapshot_digest,
                }
            }
        }

    def parse_admission_response(self, response_data: Dict[str, Any]) -> AdmissionDecision:
        """Parses the raw JSON response from the kernel admission gate."""
        decision_str = response_data.get("decision", "").upper()
        if decision_str == "AUTHORIZED":
            raw_token = response_data.get("authorizedToken", {})
            raw_rec = raw_token.get("consumptionRecord", {})
            consumption = ConsumptionRecord(
                receipt_id=raw_rec.get("receiptId", ""),
                nonce_value=raw_rec.get("nonceValue", ""),
                epoch=raw_rec.get("epoch", 1),
                sequence_number=raw_rec.get("sequenceNumber", 0),
            )
            token = AuthorizedActionToken(
                action_type=raw_token.get("actionType", ""),
                payload_digest=raw_token.get("payloadDigest", ""),
                consumption_record=consumption,
            )
            return AdmissionDecision(authorized=True, token=token)
        else:
            reason_str = response_data.get("refusalReason", "UNKNOWN")
            reason = getattr(RefusalReason, reason_str, RefusalReason.UNKNOWN)
            return AdmissionDecision(
                authorized=False,
                refusal_reason=reason,
                error_message=response_data.get("errorMessage", "Admission denied by kernel gate.")
            )

    def build_rune_parse_payload(self, raw_input: str) -> Dict[str, Any]:
        """Serializes a UGIA-001 Rune AST Parse request."""
        return {
            "protocolVersion": self.protocol_version,
            "messageType": "PARSE_RUNE",
            "payload": {
                "rawInput": raw_input
            }
        }


if __name__ == "__main__":
    client = DesktopKernelClient()
    logger.info("A.U.R.A.K.A.I. Desktop Kernel Client initialized.")

    # Demonstration of mock proposal validation
    sample_binding = SnapshotBinding(
        blueprint_digest="digest-bp-001",
        observation_digests=["obs-101", "obs-102"],
        falsification_trace_digest="trace-falsify-01",
        snapshot_digest="snapshot-verified-001"
    )
    sample_proposal = TriggerProposal(
        action_type="SUBSTRATE_EVOLUTION",
        payload_digest="snapshot-verified-001",
        epoch=1,
        nonce_value="nonce-42-hex",
        snapshot_binding=sample_binding
    )

    serialized = client.build_proposal_payload(sample_proposal)
    logger.info(f"Serialized Proposal Request:\n{json.dumps(serialized, indent=2)}")
