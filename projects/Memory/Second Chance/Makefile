SOURCES=*.java
CLASSES=osp/*/*.class
OPTS=

all: build

build: $(CLASSES)
	@(test -d osp/tmp || \
		(mkdir -p osp/tmp; make) \
		|| (echo "Something wrong: Can't create the directory for the *.class files"; exit 1))


$(CLASSES): $(SOURCES) 
	javac -g -classpath .:$(CLASSPATH):OSP.jar -d . $(SOURCES)

run: 	build
	java -classpath .:$(CLASSPATH):OSP.jar osp.OSP -noGUI $(OPTS)

gui: 	build
	java -classpath .:$(CLASSPATH):OSP.jar osp.OSP $(OPTS)

demo:
	java -classpath .:$(CLASSPATH):Demo.jar osp.OSP $(OPTS)

debug: 	build
	jdb -classpath .:$(CLASSPATH):OSP.jar osp.OSP $(OPTS)

clean:
	/bin/rm -rf osp temp *.log saved
