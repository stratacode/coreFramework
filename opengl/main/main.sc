package sc.opengl;

import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.glu.GLU;

import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import sc.opengl.CCanvas;
import sc.opengl.CRotate;
import sc.opengl.CTranslate;
import sc.opengl.CShape;
import sc.opengl.CColor;
import sc.opengl.CText;
import sc.opengl.CGroup;
import sc.opengl.CLabel;
import sc.opengl.CTree;
import sc.opengl.CTextView;
import sc.opengl.CAxis;
import sc.opengl.CRect;
import sc.opengl.CPolygonStipple;
import sc.opengl.CLineStipple;

import sc.opengl.IRenderNode;
import sc.opengl.IRenderParent;

opengl.main extends lib, swing.core, util {
   codeType = sc.layer.CodeType.Framework;

   compiledOnly = true;
}
