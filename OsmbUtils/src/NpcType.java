public class NpcType {
  public final int bottomHeight;
  public final int height;
  public final int minimumHitpointsToFight;
  public final String name;
  public final double resizeFactor;

  public NpcType(String name, int bottomHeight, int height, double resizeFactor, int minimumHitpointsToFight) {
    this.bottomHeight = bottomHeight;
    this.height = height;
    this.minimumHitpointsToFight = minimumHitpointsToFight;
    this.name = name;
    this.resizeFactor = resizeFactor;
  }
  public NpcType(String name, int height, double resizeFactor) {
    this(name, 0, height, resizeFactor, 25);
  }
}
