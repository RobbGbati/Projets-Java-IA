package com.misterbil.springai.tools;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * EXTENSION 9.4 — Les OUTILS : des méthodes Java que le LLM peut appeler.
 *
 * ─── Pourquoi des outils ? ──────────────────────────────────────────────
 * Un LLM ne sait que GÉNÉRER DU TEXTE à partir de son entraînement.
 * Il ne connaît ni l'heure qu'il est, ni l'état de ta base de données,
 * et il ne peut déclencher aucune action. Demande-lui l'heure → il
 * invente une réponse plausible. Les outils comblent exactement ça :
 * on lui donne accès à des capacités EXTERNES, implémentées en Java.
 *
 * ─── Comment le LLM "voit" ces méthodes ─────────────────────────────────
 * Spring AI lit les annotations @Tool / @ToolParam et génère une
 * description JSON de chaque méthode (nom, description, paramètres
 * typés) envoyée au fournisseur avec le prompt. Les DESCRIPTIONS sont
 * donc cruciales : c'est la SEULE chose que le modèle lit pour décider
 * s'il appelle l'outil, et avec quels arguments. Une description vague =
 * un outil mal utilisé. C'est l'équivalent IA de la javadoc... sauf
 * qu'ici, elle est exécutée.
 *
 * L'heure choisie comme exemple n'est pas un hasard : c'est le test le
 * plus simple pour PROUVER que l'outil a été appelé — un LLM sans outil
 * ne peut pas connaître l'heure réelle.
 */
public class TimeTools {

    @Tool(description = """
            Retourne la date et l'heure actuelles réelles, au fuseau horaire
            du serveur. À utiliser dès que la question porte sur la date ou
            l'heure d'aujourd'hui ou de maintenant.""")
    public String getCurrentDateTime() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm:ss", Locale.FRENCH));
    }

    @Tool(description = """
            Calcule la date exacte située un certain nombre de jours après
            aujourd'hui. À utiliser pour toute question du type "quelle date
            sera-t-on dans N jours" ou pour calculer une échéance.""")
    public String getDateInDays(
            @ToolParam(description = "Nombre de jours à ajouter à la date d'aujourd'hui")
            int days) {
        return LocalDate.now()
                .plusDays(days)
                .format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));
    }
}
