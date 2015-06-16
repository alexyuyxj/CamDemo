package m.cam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaFormat;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;

public class Recorder implements PreviewCallback {
	public static final int WIDTH = 1280;
	public static final int HEIGHT = 720;
	
	private MediaCodec encoder;
	private long startTime;
	private BufferInfo bufferInfo;
	private String path;
	private FileOutputStream fos;
	private byte[] buff;
	
	public Recorder() {
		bufferInfo = new BufferInfo();
		MediaFormat format = MediaFormat.createVideoFormat("video/avc", WIDTH, HEIGHT);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 2000000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        encoder = MediaCodec.createEncoderByType("video/avc");
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		encoder.start();
		
		try {
			path = "/sdcard/" + System.currentTimeMillis();
			fos = new FileOutputStream(path + ".h264");
			buff = new byte[WIDTH * HEIGHT * 4];
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void onPreviewFrame(byte[] data, Camera camera) {
		try {
			yvuToyuv(data);
			ByteBuffer[] inputBuffers = encoder.getInputBuffers();
			int inputBufferIndex = encoder.dequeueInputBuffer(-1);
	        if (inputBufferIndex >= 0) {
	            ByteBuffer ibb = inputBuffers[inputBufferIndex];
	            ibb.position(0);
	            ibb.put(data);
	            long framePreTimeUs;
	            if (startTime == 0) {
	            	startTime = System.nanoTime();
	            	framePreTimeUs = 0;
	            } else {
	            	framePreTimeUs = (System.nanoTime() - startTime) / 1000;
	            }
	            encoder.queueInputBuffer(inputBufferIndex, 0, data.length, framePreTimeUs, 0);
	        }

	        ByteBuffer[] outputBuffers = encoder.getOutputBuffers();
	        int outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
	        while (outputBufferIndex >= 0) {
	            ByteBuffer obb = outputBuffers[outputBufferIndex];
	            obb.get(buff, 0, bufferInfo.size);
	            fos.write(buff, 0, bufferInfo.size);

	            encoder.releaseOutputBuffer(outputBufferIndex, false);
	            outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
	        }
	        fos.flush();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private void yvuToyuv(byte[] data) {
		int start = data.length / 3 * 2;
		byte tmp;
		for (int i = start; i < data.length; i += 2) {
			tmp = data[i];
			data[i] = data[i + 1];
			data[i + 1] = tmp;
		}
	}
	
	public void stop() {
		try {
			encoder.stop();
			encoder.release();
			fos.close();
			
			H264TrackImpl track = new H264TrackImpl(new FileDataSourceImpl(path + ".h264"), "eng", 25, 1);
			Movie movie = new Movie();
			movie.addTrack(track);
			Container container = new DefaultMp4Builder().build(movie);
			fos = new FileOutputStream(path + ".mp4");
			container.writeContainer(fos.getChannel());
			fos.close();
			
			(new File(path + ".h264")).delete();
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

}
