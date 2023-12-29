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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;

public class Regrowth extends Blob {
	
	@Override
	protected void evolve() {
		super.evolve();
		
		if (volume > 0) {
			int cell;
			for (int i = area.left; i < area.right; i++) {
				for (int j = area.top; j < area.bottom; j++) {
					cell = i + j*Dungeon.level.width();
					if (needTerrainChange(cell))
						changeTerrainByConditions(cell);
				}
			}
			Dungeon.observe();
		}
	}

	private void changeTerrainByConditions(int cell) {
		int c = Dungeon.level.map[cell];
		int c1 = c;
		if (allowsGrassGrowth(c)) {
			c1 = GrassGrowth(cell);
		} else if (canUpgradeToHighGrass(c, cell)) {
			c1 = UpgradeToHighGrass();
		}

		if (isTerrainChanged(c1, c)) {
			updateMap(cell, c1);
		}

		addDebuffIfMeetTheConditions(cell);
	}

	private void addDebuffIfMeetTheConditions(int cell) {
		Char ch = Actor.findChar(cell);
		if (ch != null
				&& !ch.isImmune(this.getClass())
				&& off[cell] > 1) {
			Buff.prolong( ch, Roots.class, TICK );
		}
	}

	private void updateMap(int cell, int c1) {
		Level.set(cell, c1);
		GameScene.updateMap(cell);
	}

	private boolean isTerrainChanged(int c1, int c) {
		return c1 != c;
	}

	private int UpgradeToHighGrass() {
		int c1;
		c1 = Terrain.HIGH_GRASS;
		return c1;
	}

	private boolean canUpgradeToHighGrass(int c, int cell) {
		return (c == Terrain.GRASS || c == Terrain.FURROWED_GRASS)
				&& cur[cell] > 9 && Dungeon.level.plants.get(cell) == null && Actor.findChar(cell) == null;
	}

	private int GrassGrowth(int cell) {
		int c1;
		c1 = (cur[cell] > 9 && Actor.findChar(cell) == null)
				? Terrain.HIGH_GRASS : Terrain.GRASS;
		return c1;
	}

	private boolean allowsGrassGrowth(int c) {
		return c == Terrain.EMPTY || c == Terrain.EMBERS || c == Terrain.EMPTY_DECO;
	}

	private boolean needTerrainChange(int cell) {
		return off[cell] > 0;
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		
		emitter.start( LeafParticle.LEVEL_SPECIFIC, 0.2f, 0 );
	}
}
