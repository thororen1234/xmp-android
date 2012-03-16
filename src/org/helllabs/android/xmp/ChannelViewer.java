package org.helllabs.android.xmp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.RemoteException;

public class ChannelViewer extends Viewer {
	private Paint scopePaint, scopeLinePaint, insPaint, meterPaint, numPaint;
	private int fontSize, fontHeight, fontWidth;
	private int font2Size, font2Height, font2Width;
	private String[] insName = new String[256];		
	private int[] holdIns;
	private Rect rect = new Rect();
	private byte[] buffer;
	private int[] holdKey;
	private String channelNumber[];

	@Override
	public void setup(ModInterface modPlayer, int[] modVars) {
		super.setup(modPlayer, modVars);

		int chn = modVars[3];
		String[] instruments;

		try {
			instruments = modPlayer.getInstruments();
			for (int i = 0; i < 256; i++) {
				if (i < instruments.length) {
					insName[i] = new String(String.format("%02X %s", i, instruments[i]));
				} else {
					insName[i] = new String(String.format("%02X", i));
				}
			}
		} catch (RemoteException e) { }

		holdIns = new int[chn];
		for (int i = 0; i < chn; i++) {
			holdIns[i] = -1;
		}

		synchronized (isDown) {
			posY = 0;
		}

		holdKey = new int[chn];
		channelNumber = new String[chn];

		for (int i = 0; i < chn; i++) {
			channelNumber[i] = new String(String.format("%2d", i + 1));
		}
	}

	@Override
	public void update(Info info) {
		Canvas c = null;
		
		try {
			c = surfaceHolder.lockCanvas(null);
			synchronized (surfaceHolder) {

				doDraw(c, modPlayer, info);
			}
		} finally {
			// do this in a finally so that if an exception is thrown
			// during the above, we don't leave the Surface in an
			// inconsistent state
			if (c != null) {
				surfaceHolder.unlockCanvasAndPost(c);
			}
		}
	}

	private void doDraw(Canvas canvas, ModInterface modPlayer, Info info) {
		final int chn = modVars[3];
		final int scopeWidth = 8 * fontWidth;
		final int scopeHeight = 3 * fontHeight;
		final int scopeLeft = 2 * font2Width + 2 * fontWidth;
		final int volLeft = scopeLeft + scopeWidth + fontWidth * 2;
		final int volWidth = (canvasWidth - 6 * fontWidth - volLeft) / 2;
		final int panLeft = volLeft + volWidth + 4 * fontWidth;
		final int panWidth = volWidth;
		int biasY;

		synchronized (isDown) {
			int max = canvasHeight - (chn * 4 + 1) * fontHeight;
			biasY = deltaY + posY;

			if (max > 0) {
				max = 0;
			}

			if (biasY > 0) {
				biasY = posY = 0;
			}

			if (biasY < max) {
				biasY = max;
			}
		}

		// Clear screen
		canvas.drawColor(Color.BLACK);

		for (int i = 0; i < chn; i++) {
			final int y = biasY + (i * 4 + 1) * fontHeight;
			final int ins = info.instruments[i];
			final int vol = info.volumes[i];
			final int finalvol = info.finalvols[i];
			final int pan = info.pans[i];
			final int key = info.keys[i];
			
			if (ins >= 0) {
				holdIns[i] = ins;
			}
			
			if (key >= 0) {
				holdKey[i] = key;
			}

			// Don't draw if not visible
			if (y < -scopeHeight || y > canvasHeight) {
				continue;
			}

			// Draw channel number
			canvas.drawText(channelNumber[i], 0, y + scopeHeight / 2 + font2Height / 2, numPaint);

			// Draw scopes

			rect.set(scopeLeft, y + 1, scopeLeft + scopeWidth, y + scopeHeight);
			canvas.drawRect(rect, scopePaint);

			try {
				int trigger;

				// Be very careful here!
				// Our variables are latency-compensated but sample data is current
				// so caution is needed to avoid retrieving data using old variables
				// from a module with sample data from a newly loaded one.
				
				if (ins >= 0 || key >= 0) {
					trigger = 1;
				} else {
					trigger = 0;
				}

				modPlayer.getSampleData(trigger, holdIns[i], holdKey[i], i, scopeWidth, buffer);

			} catch (RemoteException e) { }
			for (int j = 0; j < scopeWidth; j++) {
				canvas.drawPoint(scopeLeft + j, y + scopeHeight / 2 + buffer[j] * finalvol / 64 * scopeHeight / 2 / 180, scopeLinePaint);
			}


			final int ci = holdIns[i];
			if (ci >= 0) {
				canvas.drawText(insName[ci], volLeft, y + fontHeight, insPaint);
			}

			// Draw volumes
			int volX = volLeft + vol * volWidth / 0x40;
			int volY1 = y + 2 * fontHeight;
			int volY2 = y + 2 * fontHeight + fontHeight / 3;	
			rect.set(volLeft, volY1, volX, volY2);
			canvas.drawRect(rect, meterPaint);
			rect.set(volX + 1, volY1, volLeft + volWidth, volY2);
			canvas.drawRect(rect, scopePaint);

			// Draw pan
			int panX = panLeft + panWidth / 2 + pan * panWidth / 0x100;
			rect.set(panLeft, volY1, panLeft + panWidth, volY2);
			canvas.drawRect(rect, scopePaint);
			rect.set(panX, volY1, panX + fontWidth / 2, volY2);
			canvas.drawRect(rect, meterPaint);
		}
	}

	public ChannelViewer(Context context) {
		super(context);

		fontSize = getResources().getDimensionPixelSize(R.dimen.channelview_font_size);
		font2Size = getResources().getDimensionPixelSize(R.dimen.channelview_channel_font_size);

		scopePaint = new Paint();
		scopePaint.setARGB(255, 40, 40, 40);

		scopeLinePaint = new Paint();
		scopeLinePaint.setARGB(255, 80, 160, 80);

		meterPaint = new Paint();
		meterPaint.setARGB(255, 40, 80, 160);

		insPaint = new Paint();
		insPaint.setARGB(255, 140, 140, 160);
		insPaint.setTypeface(Typeface.MONOSPACE);
		insPaint.setTextSize(fontSize);
		insPaint.setAntiAlias(true);

		numPaint = new Paint();
		numPaint.setARGB(255, 220, 220, 220);
		numPaint.setTypeface(Typeface.MONOSPACE);
		numPaint.setTextSize(font2Size);
		numPaint.setAntiAlias(true);

		fontWidth = (int)insPaint.measureText("X");
		fontHeight = fontSize * 12 / 10;

		font2Width = (int)numPaint.measureText("X");
		font2Height = font2Size * 12 / 10;

		buffer = new byte[8 * fontWidth];
	}
}
