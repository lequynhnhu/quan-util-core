package qj.util.lang;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import qj.util.FileUtil;
import qj.util.IOUtil;
import qj.util.funct.F1;

public class CompositeClassPathLoader extends SimpleClassPathClassLoader {
	LinkedList<F1<String, byte[]>> loaders = new LinkedList<F1<String, byte[]>>();

	public CompositeClassPathLoader(String... paths) {
		for (String path : paths) {
			File file = new File(path);
			
			F1<String, byte[]> loader = loader(file);
			if (loader == null) {
				throw new RuntimeException("Path not exists " + path);
			}
			loaders.add(loader);
		}
	}
	public CompositeClassPathLoader(Collection<File> paths) {
		for (File file : paths) {
			F1<String, byte[]> loader = loader(file);
			if (loader == null) {
				throw new RuntimeException("Path not exists " + file.getPath());
			}
			loaders.add(loader);
		}
	}
	

	public static F1<String, byte[]> loader(File file) {
		if (!file.exists()) {
			return null;
		} else if (file.isDirectory()) {
			return dirLoader(file);
		} else {
			try {
				final JarFile jarFile = new JarFile(file);

				return jarLoader(jarFile);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static File findFile(String filePath, File classPath) {
		File file = new File(classPath, filePath);
		return file.exists() ? file : null;
	}

	public static F1<String, byte[]> dirLoader(final File dir) {
		return new F1<String, byte[]>() {public byte[] e(String filePath) {
			File file = findFile(filePath, dir);
			if (file == null) {
				return null;
			}
			
			return FileUtil.readFileToBytes(file);
		}};
	}

	private static F1<String, byte[]> jarLoader(final JarFile jarFile) {
//		Enumeration<JarEntry> entries = jarFile.entries();
//		while (entries.hasMoreElements()) {
//			JarEntry jarEntry = (JarEntry) entries.nextElement();
//			System.out.println(jarEntry.getName());
//		}
		
		return new F1<String, byte[]>() {
			public byte[] e(String filePath) {
				ZipEntry entry = jarFile.getJarEntry(filePath);
				if (entry == null) {
					return null;
				}
				try {
					return IOUtil.readData(jarFile.getInputStream(entry));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			protected void finalize() throws Throwable {
				IOUtil.close(jarFile);
				super.finalize();
			}
		};
	}
	

	public InputStream getResourceAsStream(String name) {
//		System.out.println("Loading res " + name);
		for (F1<String, byte[]> loader : loaders) {
			byte[] data = loader.e(name);
			if (data!= null) {
				return new ByteArrayInputStream(data);
			}
		}
		return null;
	}

	@Override
	protected byte[] loadNewClass(String name) {
//		System.out.println("Loading class " + name);
		for (F1<String, byte[]> loader : loaders) {
			byte[] data = loader.e(SimpleClassPathClassLoader.toFilePath(name));
			if (data!= null) {
				return data;
			}
		}
		return null;
	}
}
