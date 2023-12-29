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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Shadows;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShaftParticle;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;

public class Foliage extends Blob {
	
	@Override
	protected void evolve() {

		int[] map = Dungeon.level.map;
		
		boolean seen = false;

		int cell;
		for (int i = area.left; i < area.right; i++) {
			for (int j = area.top; j < area.bottom; j++) {
				cell = i + j*Dungeon.level.width();
				if (hasFoliage(cell)) {

					preserveFoliage(cell);
					updateVolume(cell);

					if (terrainIsEmbers(map[cell])) {
						setTerrainToGrass(map, cell);
						updateMap(cell);
					}

					seen = checkCellAndMarkSeen(seen, cell);

				} else {
					resetFoliage(cell);
				}
			}
		}

		addBuffWhenInFoliage();

		if (seen) {
			addGardenLandmark();
		}
	}

	private void addGardenLandmark() {
		Notes.add( Notes.Landmark.GARDEN );
	}

	private void addBuffWhenInFoliage() {
		Hero hero = Dungeon.hero;
		if (hero.isAlive() && cur[hero.pos] > 0) {
			Shadows s = Buff.affect( hero, Shadows.class );
			if (s != null){
				s.prolong();
			}
		}
	}

	private void resetFoliage(int cell) {
		off[cell] = 0;
	}

	private boolean checkCellAndMarkSeen(boolean seen, int cell) {
		seen = seen || Dungeon.level.visited[cell];
		return seen;
	}

	private void updateMap(int cell) {
		GameScene.updateMap(cell);
	}

	private void setTerrainToGrass(int[] map, int cell) {
		map[cell] = Terrain.GRASS;
	}

	private boolean terrainIsEmbers(int map) {
		return map == Terrain.EMBERS;
	}

	private void preserveFoliage(int cell) {
		off[cell] = cur[cell];
	}

	private boolean hasFoliage(int cell) {
		return cur[cell] > 0;
	}

	private void updateVolume(int cell) {
		volume += off[cell];
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.start( ShaftParticle.FACTORY, 0.9f, 0 );
	}
	
	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
