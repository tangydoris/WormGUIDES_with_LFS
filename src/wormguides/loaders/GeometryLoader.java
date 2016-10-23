package wormguides.loaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * Utility that builds 3D geometry for scene element meshes to be placed in 3D subscene
 */
public class GeometryLoader {

    private static final String VERTEX_LINE = "v ";
    private static final String FACE_LINE = "f ";

    /**
     * Builds a 3D mesh from a file
     *
     * @param fileName
     *         the name of the obj fle for the mesh
     *
     * @return the 3D mesh
     */
    public static MeshView loadOBJ(final String fileName) {
        MeshView meshView = null;
        final URL url = ProductionInfoLoader.class.getResource("/" + fileName);

        final List<double[]> coords = new ArrayList<>();
        final List<int[]> faces = new ArrayList<>();

        if (url != null) {
            try (final InputStreamReader streamReader = new InputStreamReader(url.openStream());
                 final BufferedReader reader = new BufferedReader(streamReader)) {

                String line;
                StringTokenizer tokenizer;
                String v;
                String f;
                String lineType;
                while ((line = reader.readLine()) != null) {
                    // process each line in the obj file
                    lineType = line.substring(0, 2);
                    switch (lineType) {
                        case VERTEX_LINE: {
                            // process vertex lines
                            v = line.substring(2);
                            double[] vertices = new double[3];
                            int counter = 0;
                            tokenizer = new StringTokenizer(v);
                            while (tokenizer.hasMoreTokens()) {
                                vertices[counter++] = Double.parseDouble(tokenizer.nextToken());
                            }
                            // make sure good line
                            if (counter == 3) {
                                coords.add(vertices);
                            }
                            break;
                        }
                        case FACE_LINE: {
                            // process face lines
                            f = line.substring(2);
                            int[] faceCoords = new int[3];
                            int counter = 0;

                            tokenizer = new StringTokenizer(f);
                            while (tokenizer.hasMoreTokens()) {
                                faceCoords[counter++] = Integer.parseInt(tokenizer.nextToken());
                            }
                            if (counter == 3) {
                                faces.add(faceCoords);
                            }
                            break;
                        }
                        default:
                            break;
                    }
                }
                meshView = new MeshView(createMesh(coords, faces));
                reader.close();
            } catch (IOException e) {
                System.out.println("The file " + fileName + " wasn't found on the system.");
            }
        }
        return meshView;
    }

    /**
     * Builds the mesh from the loaded vertex coordinates and faces in the file
     */
    private static TriangleMesh createMesh(final List<double[]> coords, final List<int[]> faces) {
        final TriangleMesh mesh = new TriangleMesh();
        int counter = 0;
        int texCounter = 0;
        final float stripeSeparation = 1500;

        float[] coordinates = new float[(coords.size() * 3)];
        float[] texCoords = new float[(coords.size() * 2)];
        for (double[] coord : coords) {
            for (int j = 0; j < 3; j++) {
                coordinates[counter++] = (float) coord[j];
            }
            texCoords[texCounter++] = 0;
            texCoords[texCounter++] = ((float) coord[0] / stripeSeparation) * 200;
        }

        mesh.getPoints().addAll(coordinates);
        mesh.getTexCoords().addAll(texCoords);

        counter = 0;

        int[] faceCoords = new int[(faces.size() * 3) * 2];
        for (int[] face : faces) {
            for (int j = 0; j < 3; j++) {
                faceCoords[counter++] = face[j] - 1;
                faceCoords[counter++] = face[j] - 1;
                // for our texture coordinate -
                // face syntax:
                // p0, t0, p1,
                // t1, p2, t2
            }
        }

        mesh.getFaces().addAll(faceCoords);
        return mesh;
    }
}