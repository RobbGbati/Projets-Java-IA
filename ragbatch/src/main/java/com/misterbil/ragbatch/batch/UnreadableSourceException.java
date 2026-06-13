package com.misterbil.ragbatch.batch;

/**
 * Exception TYPÉE levée quand un fichier source est illisible.
 *
 * Pourquoi une classe dédiée ? Parce que la tolérance aux pannes de
 * Spring Batch (.skip(), .retry()) se pilote PAR TYPE D'EXCEPTION.
 * TextReader enrobe ses IOException dans des RuntimeException brutes :
 * déclarer .skip(RuntimeException.class) skipperait n'importe quelle
 * erreur, y compris des bugs. Une exception métier précise permet de
 * dire au framework : "CE problème-là est skippable, rien d'autre."
 */
public class UnreadableSourceException extends RuntimeException {

    public UnreadableSourceException(String source, Throwable cause) {
        super("Fichier source illisible : " + source, cause);
    }
}
