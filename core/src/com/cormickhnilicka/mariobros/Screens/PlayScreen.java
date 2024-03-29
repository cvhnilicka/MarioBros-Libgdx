package com.cormickhnilicka.mariobros.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cormickhnilicka.mariobros.MarioBros;
import com.cormickhnilicka.mariobros.Scenes.Hud;
import com.cormickhnilicka.mariobros.Sprites.Enemy;
import com.cormickhnilicka.mariobros.Sprites.Goomba;
import com.cormickhnilicka.mariobros.Sprites.Item;
import com.cormickhnilicka.mariobros.Sprites.ItemDef;
import com.cormickhnilicka.mariobros.Sprites.Mario;
import com.cormickhnilicka.mariobros.Sprites.Mushroom;
import com.cormickhnilicka.mariobros.Tools.B2WorldCreator;
import com.cormickhnilicka.mariobros.Tools.WorldContactListener;

import java.awt.TextArea;
import java.util.PriorityQueue;

public class PlayScreen implements Screen {

    private MarioBros game;
    private TextureAtlas atlas;

    private OrthographicCamera gamecam; //the camera that follows and diplays the game
    private Viewport gamePort;
    private Hud hud;

    private TmxMapLoader maploader;
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;


    // box2d variables
    private World world;
    private Box2DDebugRenderer b2dr;
    private B2WorldCreator creator;
    private Mario player;


    private Music music;

    private Array<Item> items;
    private PriorityQueue<ItemDef> itemsToSpawn;




    // constructor
    public PlayScreen(MarioBros game) {
        // for larger assests, look into libgdx asset manager
        atlas = new TextureAtlas("Mario_and_Enemies.pack");

        this.game = game;
        gamecam = new OrthographicCamera();
        // gamePort = new StretchViewport(800,480,gamecam);
        gamePort = new FitViewport(MarioBros.V_WITDH/MarioBros.PPM, MarioBros.V_HEIGHT/MarioBros.PPM, gamecam);
        // gamePort = new ScreenViewport(gamecam);
        hud = new Hud(game.batch);

        maploader = new TmxMapLoader();
        map = maploader.load("MarioMap.tmx");
        renderer = new OrthogonalTiledMapRenderer(map, 1/MarioBros.PPM);

        gamecam.position.set(gamePort.getWorldWidth()/2, gamePort.getWorldHeight()/2, 0);

        world = new World(new Vector2(0, -10), true);
        b2dr = new Box2DDebugRenderer();
        creator = new B2WorldCreator(this);
        // Create mario
        player = new Mario(this);

        world.setContactListener(new WorldContactListener());

        music = MarioBros.manager.get("audio/music/mario_music.ogg", Music.class);
        music.setLooping(true);
        music.play();

        items = new Array<Item>();
        itemsToSpawn = new PriorityQueue<ItemDef>();


    }

    public void spawnItem(ItemDef idef) {
        itemsToSpawn.add(idef);
    }

    public void handleSpawndingItems() {
        if (!itemsToSpawn.isEmpty()) {
            ItemDef idef = itemsToSpawn.poll();
            if (idef.type == Mushroom.class) {
                items.add(new Mushroom(this, idef.position.x,  idef.position.y));
            }
        }
    }
    public void handleInput(float dt){
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)){
            // impulse = immediate change
            player.b2body.applyLinearImpulse(new Vector2(0, 4f), player.b2body.getWorldCenter(), true);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) && player.b2body.getLinearVelocity().x <= 2) {
            player.b2body.applyLinearImpulse(new Vector2(0.1f, 0), player.b2body.getWorldCenter(), true);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) && player.b2body.getLinearVelocity().x >= -2) {
            player.b2body.applyLinearImpulse(new Vector2(-0.1f, 0), player.b2body.getWorldCenter(), true);
        }

    }
    public void update(float dt) {
        handleInput(dt);
        handleSpawndingItems();
        world.step(1/60f,6,2);
        // track mario with game came
        gamecam.position.x = player.b2body.getPosition().x;
        gamecam.update(); // always update every cycle

        player.update(dt);
        hud.update(dt);

        for (Enemy enemy: creator.getGoombas())
            enemy.update(dt);
        for (Item item: items)
            item.update(dt);

        renderer.setView(gamecam);
    }


    public TextureAtlas getAtlas(){
        return atlas;
    }
    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        update(delta);
        // clear the screen
        Gdx.gl.glClearColor(1,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.render();

        b2dr.render(world, gamecam.combined);

        game.batch.setProjectionMatrix(gamecam.combined);
        game.batch.begin();
        player.draw(game.batch);
        // wake up the enemy if close enough
        for (Enemy enemy: creator.getGoombas()) {
            enemy.draw(game.batch);
            if (enemy.getX() < player.getX() + 224/MarioBros.PPM)
                enemy.b2body.setActive(true);
        }
        for (Item item: items)
            item.draw(game.batch);

        game.batch.end();


        // only render what camera can see
        game.batch.setProjectionMatrix(hud.stage.getCamera().combined);
        hud.stage.draw();


    }

    @Override
    public void resize(int width, int height) {
        gamePort.update(width,height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    public TiledMap getMap() {
        return this.map;
    }

    public World getWorld() {
        return this.world;
    }

    @Override
    public void dispose() {
        map.dispose();
        renderer.dispose();
        world.dispose();
        b2dr.dispose();
        hud.dispose();

    }
}
