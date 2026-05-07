// TFC Aeronautica starter KubeJS script
// File: kubejs/server_scripts/00_tfcaero_starter_recipes.js
//
// This is intentionally light. Use it as a starting point after the base pack boots.
// Check every item ID in JEI before relying on these recipes.

ServerEvents.recipes(event => {
  // Sustainable dirt recipe from earlier discussion.
  // Mineral-heavy, not compost/soil.
  event.recipes.create.mixing(
    Item.of('minecraft:dirt', 4),
    [
      'minecraft:gravel',
      'minecraft:gravel',
      'minecraft:sand',
      'minecraft:clay_ball',
      Fluid.of('minecraft:water', 250)
    ]
  ).id('tfcaero:create_mixing_dirt_from_gravel_sand_clay')

  // Placeholder examples for later TFC/Create integration.
  // Disabled by default until we confirm TFC 1.21.1 item IDs.
  //
  // event.shaped('create:shaft', [
  //   'L',
  //   'L'
  // ], {
  //   L: '#minecraft:logs'
  // }).id('tfcaero:starter_shaft_from_lumber')
})
