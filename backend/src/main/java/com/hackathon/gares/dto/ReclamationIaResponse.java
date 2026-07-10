package com.hackathon.gares.dto;

/**
 * Reponse attendue du webhook Make.com.
 *
 * Format attendu (JSON) :
 * {
 *   "peutRepondre": true,
 *   "reponse": "Texte de la reponse a afficher au client",
 *   "raison": "optionnel, motif d'escalade si peutRepondre = false"
 * }
 *
 * Tolerance :
 * - Si "peutRepondre" est absent, on considere qu'il peut repondre si "reponse"
 *   est non vide (permet un scenario Make plus simple qui renvoie juste un texte).
 * - Si le champ "reponse" est vide/absent alors que peutRepondre = true, on
 *   considere quand meme qu'il faut escalader (pas de reponse utilisable).
 */
public record ReclamationIaResponse(
        Boolean peutRepondre,
        String reponse,
        String raison
) {
    public static ReclamationIaResponse escalade(String raison) {
        return new ReclamationIaResponse(false, null, raison);
    }

    /** Vrai uniquement si une reponse exploitable est fournie. */
    public boolean reponseAutomatiqueValide() {
        boolean aUnTexte = reponse != null && !reponse.isBlank();
        if (!aUnTexte) {
            return false;
        }
        return peutRepondre == null || peutRepondre;
    }
}
