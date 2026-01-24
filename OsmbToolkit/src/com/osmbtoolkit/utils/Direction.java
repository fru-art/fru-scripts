package com.osmbtoolkit.utils;

import com.osmb.api.utils.TileEdge;

public class Direction {
  public enum Alignment {
    ASCENDING_DIAGONAL,
    DESCENDING_DIAGONAL,
    HORIZONTAL,
    VERTICAL
  }

  public enum PrimaryDirection {
    E, N, S, W
  }

  public static Alignment convertPrimaryDirectionToAlignment(PrimaryDirection primaryDirection) {
    if (primaryDirection == PrimaryDirection.W || primaryDirection == PrimaryDirection.E) {
      return Alignment.HORIZONTAL;
    }
    return Alignment.VERTICAL;
  }

  public static Alignment convertTileEdgeToAlignment(TileEdge tileEdge) {
    if (tileEdge == TileEdge.SW || tileEdge == TileEdge.NE) return Alignment.ASCENDING_DIAGONAL;
    if (tileEdge == TileEdge.NW || tileEdge == TileEdge.SE) return Alignment.DESCENDING_DIAGONAL;
    if (tileEdge == TileEdge.W || tileEdge == TileEdge.E) return Alignment.HORIZONTAL;
    return Alignment.VERTICAL;
  }
}
