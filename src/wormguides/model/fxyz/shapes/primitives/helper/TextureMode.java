package wormguides.model.fxyz.shapes.primitives.helper;

/*
 * Copyright (C) 2013-2015 F(X)yz, 
 * Sean Phillips, Jason Pollastrini and Jose Pereda
 * All rights reserved.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.function.Function;
import javafx.scene.paint.Color;
import wormguides.model.fxyz.geometry.Point3D;
import wormguides.model.fxyz.utils.Palette.ColorPalette;
import wormguides.model.fxyz.utils.Patterns.CarbonPatterns;

/**
 *
 * @author usuario
 */
public interface TextureMode {
    
    public void setTextureModeNone();
    public void setTextureModeNone(Color color);
    public void setTextureModeNone(Color color, String image);
    public void setTextureModeImage(String image);
    public void setTextureModePattern(CarbonPatterns pattern, double scale);
    public void setTextureModeVertices3D(int colors, Function<Point3D, Number> dens);
    public void setTextureModeVertices3D(ColorPalette palette, int colors, Function<Point3D, Number> dens);
    public void setTextureModeVertices3D(int colors, Function<Point3D, Number> dens, double min, double max);
    public void setTextureModeVertices1D(int colors, Function<Number, Number> function);
    public void setTextureModeVertices1D(ColorPalette palette, int colors, Function<Number, Number> function);
    public void setTextureModeVertices1D(int colors, Function<Number, Number> function, double min, double max);
    public void setTextureModeFaces(int colors);
    public void setTextureModeFaces(ColorPalette palette, int colors);
}