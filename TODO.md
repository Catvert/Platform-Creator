# TODO

## A FAIRE

- Ajouter la possibilité à stateAction d'afficher la liste des actions à la place de l'index sous forme de slider
- Traduire tout le texte visible en français
- Javadoc + commentaires

## BUGS

- Logo du jeu problème lors du redimensionnement fenêtre (+fullscreen?)

## IDÉE
- Utiliser IMGUI pour l'UI de l'éditeur

- Ajout du pseudo de l'utilisateur au premier lancement
- Ajouter les ressources par niveau(pour les textures, sons..)
- Utiliser une bdd(json) pour la langue du jeu(anglais et français), avec la même forme que le GameKeys
- [Optimisation] Créer une sorte de préfab par gameObject ayant les mêmes caractéristiques, ce qui pourrait améliorer les performances lors du chargement(dans le niveau, il y aurait une liste de "prefabs" et une liste de pos+numPrefab)
- [Optimisation] de la taille de la matrix en invitant à l'utitilisateur à cliquer sur un rectangle qui fait la taille de cell\*hauteur pour la droite et largeur\*cell pour le haut, comme ça la taille de la matrix correspondra au besoin de l'utilisateur
 