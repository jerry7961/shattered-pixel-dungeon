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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.MagicalFireRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

public class Freezing extends Blob {
	
	@Override
	protected void evolve() {
		
		int cell;
		
		Fire fire = (Fire)Dungeon.level.blobs.get( Fire.class );
		
		for (int i = area.left-1; i <= area.right; i++) {
			for (int j = area.top-1; j <= area.bottom; j++) {
				cell = i + j*Dungeon.level.width();
				if (hasIce(cell)) {

					if (FireFreezeInteraction(fire, cell))
						continue;

					Freezing.freeze(cell);

					decreaseIce(cell);
					updateVolume(cell);
				} else {
					resetFreezing(cell);
				}
			}
		}
	}

	private void resetFreezing(int cell) {
		off[cell] = 0;
	}

	private void decreaseIce(int cell) {
		off[cell] = cur[cell] - 1;
	}

	private boolean FireFreezeInteraction(Fire fire, int cell) {
		if (fire != null && fire.volume > 0 && fire.cur[cell] > 0){
			fire.clear(cell);
			off[cell] = cur[cell] = 0;
			return true;
		}
		return false;
	}

	private boolean hasIce(int cell) {
		return cur[cell] > 0;
	}

	private void updateVolume(int cell) {
		volume += off[cell];
	}

	public static void freeze( int cell ){
		Char ch = Actor.findChar( cell );
		if (charNotImmune(ch)) {
			if (hasFrostBuff(ch)){
				extendFrostBuffDuration(ch);
			} else {
				Chill chill = ch.buff(Chill.class);
				float turnsToAdd = Dungeon.level.water[cell] ? 5f : 3f;
				turnsToAdd = getTurnsToAdd(chill, ch, turnsToAdd);
				addChillBuffIfTurnsRemaining(turnsToAdd, ch);
				upgradeChillToFrostIfCooledDown(chill, ch);
			}
		}
		
		Heap heap = Dungeon.level.heaps.get( cell );
		if (heap != null) heap.freeze();
	}

	private static void upgradeChillToFrostIfCooledDown(Chill chill, Char ch) {
		if (chill != null
				&& chill.cooldown() >= Chill.DURATION &&
				!ch.isImmune(Frost.class)){
			Buff.affect(ch, Frost.class, Frost.DURATION);
		}
	}

	private static void addChillBuffIfTurnsRemaining(float turnsToAdd, Char ch) {
		if (turnsToAdd > 0f) {
			Buff.affect(ch, Chill.class, turnsToAdd);
		}
	}

	private static float getTurnsToAdd(Chill chill, Char ch, float turnsToAdd) {
		if (chill != null){
			float chillToCap = Chill.DURATION - chill.cooldown();
			chillToCap /= ch.resist(Chill.class); //account for resistance to chill
			turnsToAdd = Math.min(turnsToAdd, chillToCap);
		}
		return turnsToAdd;
	}

	private static void extendFrostBuffDuration(Char ch) {
		Buff.affect(ch, Frost.class, 2f);
	}

	private static boolean hasFrostBuff(Char ch) {
		return ch.buff(Frost.class) != null;
	}

	private static boolean charNotImmune(Char ch) {
		return ch != null && !ch.isImmune(Freezing.class);
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.start( SnowParticle.FACTORY, 0.05f, 0 );
	}
	
	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
	
	//legacy functionality from before this was a proper blob. Returns true if this cell is visible
	public static boolean affect( int cell ) {

		affectChar(cell);

		clearFireIfPresent(cell);
		clearEternalFireIfPresent(cell);

		freezeHeapIfPresent(cell);

		return emitSnowParticleIfInHeroFOV(cell);
	}

	private static boolean emitSnowParticleIfInHeroFOV(int cell) {
		if (Dungeon.level.heroFOV[cell]) {
			CellEmitter.get(cell).start( SnowParticle.FACTORY, 0.2f, 6 );
			return true;
		} else {
			return false;
		}
	}

	private static void freezeHeapIfPresent(int cell) {
		Heap heap = Dungeon.level.heaps.get(cell);
		if (heap != null) {
			heap.freeze();
		}
	}

	private static void affectChar(int cell) {
		Char ch = Actor.findChar(cell);
		if (ch != null) {
			if (Dungeon.level.water[ch.pos]){
				Buff.prolong(ch, Frost.class, Frost.DURATION * 3);
			} else {
				Buff.prolong(ch, Frost.class, Frost.DURATION);
			}
		}
	}

	private static void clearEternalFireIfPresent(int cell) {
		MagicalFireRoom.EternalFire eternalFire = (MagicalFireRoom.EternalFire)Dungeon.level.blobs.get(MagicalFireRoom.EternalFire.class);
		if (eternalFire != null && eternalFire.volume > 0) {
			eternalFire.clear(cell);
		}
	}

	private static void clearFireIfPresent(int cell) {
		Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);
		if (fire != null && fire.volume > 0) {
			fire.clear(cell);
		}
	}
}

