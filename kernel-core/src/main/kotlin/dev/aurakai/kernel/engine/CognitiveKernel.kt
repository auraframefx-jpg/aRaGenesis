package dev.aurakai.kernel.engine

import dev.aurakai.kernel.domain.*
import java.security.MessageDigest

open class CognitiveKernel(
    private val relationshipClassifier: RelationshipClassifier,
    private val independenceResolver: IndependenceResolver,
    private val falsifyEngine: FalsifyEngine,
    private val verificationGate: VerificationGate
) {

    fun ingest(
        targetClaimId: String,
        targetClaimText: String,
        targetLevel: EpistemicLevel,
        untrustedNodeResults: List<NodeResult>
    ): GateResult {

        // 1. Flatten all raw evidence submitted across all Nexus nodes
        val rawEvidencePool = untrustedNodeResults.flatMap { it.evidence }

        // 2. Engine-owned relationship & independence resolution
        val resolvedItems = rawEvidencePool.map { raw ->
            ResolvedEvidenceItem(
                rawItem = raw,
                provenanceHash = sha256(raw.rawContent),
                independenceGroupId = independenceResolver.resolveGroup(raw),
                relationship = relationshipClassifier.classifyRelationship(targetClaimText, raw),
                effectiveWeight = deriveEngineWeight(raw)
            )
        }

        // 3. Independent anti-consensus grouping (collapse internal echo chambers)
        val groupedRepresentatives = resolvedItems
            .groupBy { it.independenceGroupId }
            .values
            .map { group -> group.maxBy { it.effectiveWeight } }

        val supporting = groupedRepresentatives.filter { it.relationship == EvidenceRelationship.SUPPORTS }
        val contradicting = groupedRepresentatives.filter { it.relationship == EvidenceRelationship.CONTRADICTS }

        // 4. Run adversarial falsification challenge
        val falsificationResult = falsifyEngine.challenge(
            claim = targetClaimText,
            items = resolvedItems
        )

        // 5. Construct immutable candidate payload
        val candidate = EvaluatedClaim(
            claimId = targetClaimId,
            claimText = targetClaimText,
            targetLevel = targetLevel,
            hasIndependentEvidence = groupedRepresentatives.size >= 2,
            hasIndependentContradiction = contradicting.isNotEmpty(),
            reliesSolelyOnCanon = groupedRepresentatives.all { it.independenceGroupId == "origin-canon-core" },
            falsificationResult = falsificationResult,
            runtimeReceipts = untrustedNodeResults.flatMap { it.runtimeReceipts },
            causalReceipts = untrustedNodeResults.flatMap { it.causalReceipts }
        )

        // 6. VerificationGate makes final state commitment
        return verificationGate.evaluate(candidate)
    }

    private fun deriveEngineWeight(item: RawEvidenceItem): Double {
        return when (item.claimedSourceType) {
            SourceType.EXTERNAL_AUDIT -> 0.95
            SourceType.CONFIG -> 0.30
            SourceType.CANON, SourceType.INTERNAL -> 0.25
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.encodeToByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

interface RelationshipClassifier {
    fun classifyRelationship(targetClaim: String, item: RawEvidenceItem): EvidenceRelationship
}

interface IndependenceResolver {
    fun resolveGroup(item: RawEvidenceItem): String
}

interface FalsifyEngine {
    fun challenge(claim: String, items: List<ResolvedEvidenceItem>): FalsificationResult
}
