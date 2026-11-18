"""Crée un compte de démonstration par rôle (journaliste, rédacteur, lecteur), via l'API.

Rejouable sans doublon : un compte déjà inscrit est simplement remis au bon rôle.
DÉMONSTRATION UNIQUEMENT : mots de passe connus, ne jamais lancer contre la production.

    python scripts/demo/creer-comptes-demo.py                 # API de dev http://localhost:8081/api
    python scripts/demo/creer-comptes-demo.py http://localhost:8088/api

Comptes créés (mot de passe identique pour les trois : Demo2026!) :
    journaliste@echomigrant.local  -> journaliste : écrit, modifie et supprime tous les articles
    redacteur@echomigrant.local    -> rédacteur   : écrit et modifie SES articles, ne supprime pas
    lecteur@echomigrant.local      -> lecteur     : lit et commente (rôle donné à toute inscription)
L'admin reste celui de config/DataInitializer : admin@echomigrant.local / ChangeMoi123!
"""
import json
import sys
import urllib.error
import urllib.request

API = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8081/api"
ADMIN = ("admin@echomigrant.local", "ChangeMoi123!")
MOT_DE_PASSE_DEMO = "Demo2026!"
COMPTES = [
    ("journaliste@echomigrant.local", "Camille", "Martin", "journaliste"),
    ("redacteur@echomigrant.local", "Yanis", "Benali", "redacteur"),
    ("lecteur@echomigrant.local", "Léa", "Durand", "lecteur"),
]


def appel(methode, chemin, corps=None, jeton=None):
    requete = urllib.request.Request(
        API + chemin,
        method=methode,
        data=json.dumps(corps).encode("utf-8") if corps is not None else None,
        headers={"Content-Type": "application/json; charset=utf-8"},
    )
    if jeton:
        requete.add_header("Authorization", "Bearer " + jeton)
    try:
        with urllib.request.urlopen(requete) as reponse:
            texte = reponse.read().decode("utf-8")
            return reponse.status, json.loads(texte) if texte else None
    except urllib.error.HTTPError as erreur:
        return erreur.code, json.loads(erreur.read().decode("utf-8") or "null")


def main():
    statut, reponse = appel("POST", "/auth/login", {"email": ADMIN[0], "password": ADMIN[1]})
    if statut != 200:
        sys.exit(f"Connexion admin impossible ({statut}) : le backend tourne-t-il sur {API} ?")
    jeton_admin = reponse["token"]

    for email, prenom, nom, role in COMPTES:
        statut, reponse = appel("POST", "/auth/register",
                                {"email": email, "prenom": prenom, "nom": nom, "password": MOT_DE_PASSE_DEMO})
        etat = "créé" if statut == 201 else "déjà présent"
        _, utilisateurs = appel("GET", "/utilisateurs", jeton=jeton_admin)
        utilisateur = next(u for u in utilisateurs if u["email"] == email)
        statut, _ = appel("PUT", f"/utilisateurs/{utilisateur['id']}/roles", {"roles": [role]}, jeton_admin)
        if statut != 200:
            sys.exit(f"Impossible de donner le rôle {role} à {email} ({statut})")
        print(f"  {email:<32} {role:<12} {etat}")

    print(f"Mot de passe des comptes de démo : {MOT_DE_PASSE_DEMO}")


if __name__ == "__main__":
    main()
