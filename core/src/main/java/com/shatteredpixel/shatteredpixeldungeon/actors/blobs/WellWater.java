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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public abstract class WellWater extends Blob {

	@Override
	protected void evolve() {
		manageNotes(processAreaAndCheckCells());
	}

	private void manageNotes(boolean seen) {
		if (seen){
			Notes.add(record());
		} else {
			Notes.remove(record());
		}
	}

	private boolean processAreaAndCheckCells() {
		int cell;
		for (int i=area.top-1; i <= area.bottom; i++) {
			for (int j = area.left-1; j <= area.right; j++) {
				cell = j + i* Dungeon.level.width();
				if (Dungeon.level.insideMap(cell)) {
					processCell(cell);
					if (shouldMarkAsSeen(cell)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean shouldMarkAsSeen(int cell) {
		return off[cell] > 0 && Dungeon.level.visited[cell];
	}

	private void processCell(int cell) {
		off[cell] = cur[cell];
		volume += off[cell];
	}

	protected boolean affect( int pos ) {
		
		Heap heap;
		
		if (heroAffected(pos)) {
			
			cur[pos] = 0;
			return true;
			
		} else if ((heap = Dungeon.level.heaps.get( pos )) != null) {
			
			Item oldItem = heap.peek();
			Item newItem = affectItem( oldItem, pos );

			if (newItem != null) {

				if (newItem == oldItem) {

				} else if (oldItem.quantity() > 1) {

					handleMultipleQuantity(oldItem, heap, newItem);

				} else {
					handleSingleQuantity(heap, oldItem, newItem);
				}

				heap.sprite.link();
				cur[pos] = 0;

				return true;

			} else {

				relocateItem(pos, heap);
				return false;

			}

		} else {
			
			return false;
			
		}
	}

	private void relocateItem(int pos, Heap heap) {
		int newPlace;
		do {
			newPlace = findRandomAdjacentPosition(pos);
		} while (!isPassableAndNotAvoided(newPlace));
		Dungeon.level.drop( heap.pickUp(), newPlace ).sprite.drop(pos);
	}

	private boolean isPassableAndNotAvoided(int newPlace) {
		return Dungeon.level.passable[newPlace] && !Dungeon.level.avoid[newPlace];
	}

	private int findRandomAdjacentPosition(int pos) {
		return pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
	}

	private void handleSingleQuantity(Heap heap, Item oldItem, Item newItem) {
		heap.replace(oldItem, newItem);
	}

	private void handleMultipleQuantity(Item oldItem, Heap heap, Item newItem) {
		oldItem.quantity( oldItem.quantity() - 1 );
		heap.drop(newItem);
	}

	private boolean heroAffected(int pos) {
		return pos == Dungeon.hero.pos && affectHero(Dungeon.hero);
	}

	protected abstract boolean affectHero( Hero hero );
	
	protected abstract Item affectItem( Item item, int pos );
	
	protected abstract Notes.Landmark record();
	
	public static void affectCell( int cell ) {
		
		Class<?>[] waters = {WaterOfHealth.class, WaterOfAwareness.class};
		
		for (Class<?>waterClass : waters) {
			WellWater water = (WellWater)Dungeon.level.blobs.get( waterClass );
			if (water != null &&
				water.volume > 0 &&
				water.cur[cell] > 0 &&
				water.affect( cell )) {
				
				Level.set( cell, Terrain.EMPTY_WELL );
				GameScene.updateMap( cell );
				
				return;
			}
		}
	}
}
