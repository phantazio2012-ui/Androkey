# Clavier Plein Écran

Clavier plein écran pour tablette/téléphone Android, connectable en Bluetooth à un ordinateur
(l'appareil se présente comme un clavier HID). Dispositions AZERTY / QWERTY / QWERTZ, thèmes de couleur.
L'interface est dans `app/src/main/assets/keyboard.html` (embarquée dans l'appli via WebView).

## Déployer sur GitHub

```bash
cd clavier-android
git init -b main
git add .
git commit -m "Clavier plein écran"
git remote add origin https://github.com/<votre-compte>/<depot>.git
git push -u origin main
```

Deux workflows se lancent automatiquement (onglet **Actions**) :

- **Build APK** : compile l'appli ; l'APK est téléchargeable dans les *Artifacts* du run (`clavier-debug-apk`).
- **Deploy démo web** : publie l'aperçu de l'interface sur GitHub Pages
  (à activer une fois : **Settings > Pages > Source : GitHub Actions**). Le Bluetooth y est simulé.

## Générer l'APK en local
Ouvrir le dossier dans Android Studio, puis **Build > Build APK(s)** →
`app/build/outputs/apk/debug/app-debug.apk`. Pour une version signée : **Build > Generate Signed Bundle / APK**.

## Fonctions
- Clavier AZERTY / QWERTY / QWERTZ avec Maj, Verr Maj, Ctrl, Alt, Alt Gr, Windows (touches collantes : appuyer puis la touche suivante).
- **PAVÉ NUMÉRIQUE** : chiffres et opérateurs (+ − × / = % ( ) , .).
- **MODE SOURIS** : pavé tactile (toucher = clic gauche, 2 doigts = clic droit / défilement), boutons gauche/droit,
  bande **molette**, et **⟲ Mouvement** (souris par gyroscope : tourner/incliner le téléphone déplace le curseur).
- Le bouton portant le logo CGSI ouvre https://cgsi.netlify.app/.

## Utilisation
1. Installer l'APK (Android 9 minimum), accepter les permissions Bluetooth.
2. Appairer l'appareil avec l'ordinateur (ou **Paramètres > Rendre visible**).
3. **Paramètres > Appareil Bluetooth** : choisir l'ordinateur, puis **Connecter**.
4. Choisir la même disposition que celle de l'ordinateur.

## Limites connues
- Certains téléphones (selon la marque/ROM) bloquent le profil HID Device.
- Couche symboles mappée pour les dispositions US, FR et DE ; à tester sur votre ordinateur.
- Pas d'iPhone/iPad.
