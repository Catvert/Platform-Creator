# TODO

## A FAIRE

- Ajouter la possibilité à stateAction d'afficher la liste des actions à la place de l'index sous forme de slider
- Traduire tout le texte visible en français
- Javadoc + commentaires
- Refactoriser findGameObjectUnderMouse() -> ajouter méthode pour récupérer cell à un point précis
- [Optimisation] Texture, Atlas et Animation qui charge les assets depuis l'assetManager toutes les frames dans imgui
## BUGS

- Logo du jeu problème lors du redimensionnement fenêtre (+fullscreen?)

## IDÉE
- Ajout du pseudo de l'utilisateur au premier lancement
- [Optimisation] de la taille de la matrix en invitant à l'utitilisateur à cliquer sur un rectangle qui fait la taille de cell\*hauteur pour la droite et largeur\*cell pour le haut, comme ça la taille de la matrix correspondra au besoin de l'utilisateur
 