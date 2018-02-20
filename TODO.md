# TODO

## A FAIRE

- Javadoc + commentaires
- Ajouter la possibilité de supprimer une ressource importé
- Écran de chargement d'un niveau
- Utiliser un i18n pour les descriptions des props des components/actions (à la place de la description en str mettre : desc_physics_movementType par exemple)

## BUGS

- Si le joueur est supprimé mais toujours en follow camera(ou autre genre en gameObjectAction) -> présent mais pas dans la matrix
- Si hauteur > x ( haut a plus de 10 cell à mon avis ), possible crash depuis le TweenSystem.update si c'est des pièces par exemples.
- Bug delta time trop grand (physics problem)

## IDÉE

- Place order pour l'expose editor 
- Sélecteur de skin basé sur la couleur (colorpicker)
- Structure, permet de créer une structure prédéfinie ou créer. Un peu à la méthode des prefabs sauf que c'est plusieurs gameObjects placé a côté de l'autre
- Ajout du pseudo de l'utilisateur au premier lancement
- Ajouter la possibilité d'importer des atlas .xml
- Permettre de redimensionner automatiquement une texture ayant des pixels transparents pour permettre de meilleurs collisions.