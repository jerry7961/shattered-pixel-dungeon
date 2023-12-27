/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2023 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;

public class Fire extends Blob {

	@Override
	protected void evolve() {

		boolean[] flamable = Dungeon.level.flamable;
		int cell;
		int fire;
		
		Freezing freeze = (Freezing)Dungeon.level.blobs.get( Freezing.class );

		boolean observe = false;

		for (int i = area.left-1; i <= area.right; i++) {
			for (int j = area.top-1; j <= area.bottom; j++) {
				cell = i + j*Dungeon.level.width();
				if (hasFire(cell)) {
					if (FireFreezeInteraction(freeze, cell))
						continue;
					burn( cell );
					fire = decreaseFire(cell);
					if (isBurnedOutAndCombustible(fire, flamable[cell]))
						observe = burnDownAndUpdateMap(cell);
				} else if (flamable[cell] && noFreeze(freeze, cell) && hasBurningNeighbor(flamable, cell)) {
					fire = getFireFromNeighbor(cell, i, j);
				} else {
					fire = 0;
				}
				updateVolume(cell, fire);
			}
		}
		if (observe) Dungeon.observe();
	}

	private void updateVolume(int cell, int fire) {
		volume += (off[cell] = fire);
	}

	private int decreaseFire(int cell) {
		int fire;
		fire = cur[cell] - 1;
		return fire;
	}

	private boolean hasFire(int cell) {
		return cur[cell] > 0;
	}

	private int getFireFromNeighbor(int cell, int i, int j) {
		int fire;
		fire = 4;
		burn(cell);
		area.union(i, j);
		return fire;
	}

	private boolean isBurnedOutAndCombustible(int fire, boolean flamable) {
		return fire <= 0 && flamable;
	}

	private boolean noFreeze(Freezing freeze, int cell) {
		return freeze == null || freeze.volume <= 0 || freeze.cur[cell] <= 0;
	}

	private boolean hasBurningNeighbor(boolean[] flamable, int cell) {
		return  (hasFire(cell - 1)
				|| hasFire(cell + 1)
				|| hasFire(cell - Dungeon.level.width())
				|| hasFire(cell + Dungeon.level.width()));
	}

	private boolean burnDownAndUpdateMap(int cell) {
		Dungeon.level.destroy(cell);
		GameScene.updateMap(cell);
		return true;
	}

	private boolean FireFreezeInteraction(Freezing freeze, int cell) {
		if (freeze != null && freeze.volume > 0 && freeze.cur[cell] > 0){
			freeze.clear(cell);
			off[cell] = cur[cell] = 0;
			return true;
		}
		return false;
	}

	public static void burn( int pos ) {
		Char ch = Actor.findChar( pos );
		if (ch != null && !ch.isImmune(Fire.class)) {
			Buff.affect( ch, Burning.class ).reignite( ch );
		}
		
		Heap heap = Dungeon.level.heaps.get( pos );
		if (heap != null) {
			heap.burn();
		}

		Plant plant = Dungeon.level.plants.get( pos );
		if (plant != null){
			plant.wither();
		}
	}
	
	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.pour( FlameParticle.FACTORY, 0.03f );
	}
	
	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
