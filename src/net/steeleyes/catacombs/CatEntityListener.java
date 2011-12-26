/*  This file is part of Catacombs.

    Catacombs is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Catacombs is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Catacombs.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author John Keay  <>(@Steeleyes, @Blockhead2)
 * @copyright Copyright (C) 2011
 * @license GNU GPL <http://www.gnu.org/licenses/>
*/
package net.steeleyes.catacombs;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EndermanPickupEvent;
import org.bukkit.event.entity.EndermanPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.Location;

import org.bukkit.entity.CreatureType;
import org.bukkit.entity.LivingEntity;

import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class CatEntityListener extends EntityListener {
  private static Catacombs plugin;

  public CatEntityListener(Catacombs instance) {
    plugin = instance;
  }

  @Override
  public void onEntityDeath(EntityDeathEvent evt) {
    LivingEntity damagee = (LivingEntity) evt.getEntity();
    Block blk = damagee.getLocation().getBlock();
    Boolean inDungeon =   plugin.prot.isInRaw(blk);
    
    //Is the monster managed?
    if(plugin.cnf.AdvancedCombat()) {
      if(plugin.monsters.isManaged(damagee)) {
        CatMob mob = plugin.monsters.get(damagee);
//        if(mob.getCreature() == CatCreature.SILVERFISH && CatUtils.Chance(30)) {
//          for(int i=0;i<2;i++) {
//            CatMob mob2 = new CatMob(plugin.cnf,CatCreature.SILVERFISH,blk.getWorld(),blk.getLocation());
//            plugin.monsters.add(mob2);
//          }
//        }
        //System.out.println("[Catacombs] Entity death (adv) "+evt.getEntity() +" "+mob+" "+mob.getHealth());
        plugin.monsters.remove(damagee);
        mob.death(evt); 
      } else if(evt instanceof PlayerDeathEvent) {
        PlayerDeathEvent pevt = (PlayerDeathEvent) evt;
        Player player = (Player) damagee;
        if(inDungeon) {
          pevt.setDroppedExp(0);
          int expLevel = player.getLevel();
          pevt.setNewExp((int)(7.0*expLevel*plugin.cnf.DeathExpKept()));
          //plugin.players.saveGear(player,evt.getDrops());
          plugin.players.saveGear(player);
          evt.getDrops().clear(); // We'll handle to items, don't drop them
        }
        plugin.monsters.removeThreat(player);
      }
    } else {
      if(inDungeon) {
        EntityDamageEvent ev = damagee.getLastDamageCause();
        Entity damager = CatUtils.getDamager(ev);
        if(damager instanceof Player) {
          int gold = plugin.cnf.Gold();
          String bal = CatUtils.giveCash(damager,gold);
          if(bal!=null)
            ((Player)damager).sendMessage(gold+" coins ("+bal+")");
        } 
      }
    }   
  }  
  
  @Override
  public void onEntityDamage(EntityDamageEvent evt) {
    if(evt.isCancelled())
      return;

    if(!(evt.getEntity() instanceof LivingEntity))
      return;
    
    LivingEntity damagee = (LivingEntity) evt.getEntity();
    
    //Is the target a managed monster?
    if(plugin.monsters.isManaged(damagee)) {
      // Projectiles cause 2 events at the moment.
      if(evt.getCause() == DamageCause.PROJECTILE && evt.getDamage() == 0)
        return;
      plugin.monsters.playerHits(evt);
    } else {  
      if(damagee instanceof Player) {
        plugin.monsters.monsterHits(evt);
      }
    }
  }
  
  @Override
  public void onEntityTarget(EntityTargetEvent evt) {
    if(evt.isCancelled())
      return;
    
    LivingEntity damagee = (LivingEntity) evt.getEntity();
    if(plugin.monsters.isManaged(damagee)) {
      CatMob mob = plugin.monsters.get(damagee);
      mob.target(evt);
      
      // If target is dead then remove from hate list
      // Check player going out of range works
      
      //TargetReason reason = evt.getReason();
      //Entity target = evt.getTarget();
      //System.out.println("[Catacombs] (cancel) Retarget "+target+" "+reason);
      //System.out.println("[Catacombs] Cancel re-target "+damagee);
      //evt.setCancelled(true);
    }

  }

  
  @Override
  public void onCreatureSpawn(CreatureSpawnEvent evt) {
    if(evt.isCancelled())
      return;
    
    Block blk = evt.getLocation().getBlock();
    CatCuboid cube = plugin.prot.getCube(blk);
    
    if(cube == null) // Not in dungeon
      return;
    
    if(!cube.isEnabled()) {  // Cancel spawns in suspended dungeons
      evt.setCancelled(true);
      return;
    }   

    // In enabled dungeon
    // Prevent creatures spawning from spawners in good light (WOLVES, PIGMEN, BLAZE mostly)
    if(evt.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER &&
       blk.getLightLevel()>10) {
      evt.setCancelled(true);
      return;         
    }
    
    if(!(evt.getEntity() instanceof LivingEntity))
      return;
       
    LivingEntity ent = (LivingEntity) evt.getEntity();
    
    // No hook yet to prevent the smooth_stone dungeons getting trashed by this yet
//    if(evt.getCreatureType() == CreatureType.SILVERFISH) {
//      if(evt.getSpawnReason() == SpawnReason.CUSTOM && !plugin.monsters.isManaged(ent)) {
//        System.out.println("[Catacombs] Cancel Silverfish spawn "+evt.getSpawnReason());
//        evt.setCancelled(true);
//        return;
//      }
//    }
    
    CatConfig cnf = plugin.cnf;
    if(cnf.AdvancedCombat()) {
      SpawnReason reason = evt.getSpawnReason();

      if(reason == SpawnReason.CUSTOM || plugin.monsters.isManaged(ent)) { // The mob is already on the list
        return;
      }
      
      // Cancel the dungeon spawn if nobody is close
      int num_players = CatUtils.countPlayerNear(ent,cnf.SpawnRadius(),cnf.SpawnDepth());
      if(num_players==0) {
        evt.setCancelled(true);
        return;
      }
      
      Boolean isSilverFish = (evt.getCreatureType() == CreatureType.SILVERFISH);
      
      int num_mobs = CatUtils.countCreatureNear(ent, cnf.MonsterRadius(), 2);
      //System.out.println("[Catacombs] spawn players="+num_players+" mobs="+num_mobs+" size="+plugin.monsters.size());
      if(!isSilverFish && num_mobs >= cnf.MonsterMax()*num_players) {
        evt.setCancelled(true);
        return;
      }      

      //Location loc = evt.getLocation();
      CatMob mob = new CatMob(plugin.cnf,evt.getCreatureType(),ent);
      plugin.monsters.add(mob);

      //CatMob mob2 = new CatMob(CatCreature.CHICKEN,loc.getWorld(),loc);
      //plugin.monsters.add(mob2);
      //System.out.println("[Catacombs] "+plugin.monsters.size()+" "+ent);
    } else {
      CreatureType t = evt.getCreatureType();
      if(t == CreatureType.WOLF)
        ((Wolf)ent).setAngry(true);
        
      if(t == CreatureType.PIG_ZOMBIE)
        ((PigZombie)ent).setAngry(true);
    }
  }

  @Override
  public void onEntityExplode(EntityExplodeEvent eEvent){
    if(eEvent.isCancelled())
      return;
   
    Location loc = eEvent.getLocation();
    Block blk = loc.getBlock();
    List<Block> list = eEvent.blockList();

    if(plugin.prot.isProtected(blk) || any_protected(list)) {
      list.clear();
    }
  }
  
  private Boolean any_protected(List<Block> list) {
    for(Block b : list) {
      if(plugin.prot.isProtected(b)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onEndermanPickup(EndermanPickupEvent eEvent) {
    if(eEvent.isCancelled())
      return;

    Block blk = eEvent.getBlock();
    if(plugin.prot.isProtected(blk))
      eEvent.setCancelled(true);
  }
  
  @Override
  public void onEndermanPlace(EndermanPlaceEvent eEvent) {
    if(eEvent.isCancelled())
      return;

    Block blk = eEvent.getLocation().getBlock();
    if(plugin.prot.isProtected(blk))
      eEvent.setCancelled(true);
  }
}
