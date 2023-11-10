# Location of trees:
SOURCE_DIR := src/build
SOURCE_TEST_DIR := src/test
OUTPUT_DIR := dest

# Unix tools
AWK := awk
FIND := find
MKDIR := mkdir -p
RM := rm -rf
SHELL := /bin/bash

# Set the Java classpath
class_path := OUTPUT_DIR

# Java tools
JC := javac
JAVA := java
JFLAGS := -sourcepath $(SOURCE_DIR) \
-d $(OUTPUT_DIR)
JVMFLAGS := -cp $(OUTPUT_DIR)
TESTFLAGS := -cp lib/junit-4.13.2.jar
JVM := $(JAVA) $(JVMFLAGS)

# Set the java CLASSPATH
class_path := OUTPUT_DIR

# make-directories - Ensure output directory exists.
make-directories := $(shell $(MKDIR) $(OUTPUT_DIR))

# all - Perform all tasks for a complete build
.PHONY: compile
all: compile

all_javas := $(OUTPUT_DIR)/all.javas
all_tests := $(OUTPUT_DIR)/all.tests

# compile - Compile the source
.PHONY: compile
compile: $(all_javas)
	$(JC) $(JFLAGS) @$<

# all_javas - Gather source file list
.INTERMEDIATE: $(all_javas)
$(all_javas):
	$(FIND) $(SOURCE_DIR) -name '*.java' > $@


.PHONY: clean
clean:
	$(RM) $(OUTPUT_DIR)
	rm -f clock
	rm -f localStorage.txt

.PHONY: cleanall
cleanall:
	$(RM) $(OUTPUT_DIR)
	rm -f clock
	rm -f localStorage.txt
	rm -rf randInputs

.PHONY: classpath
classpath:
	@echo CLASSPATH='$(CLASSPATH)'

.PHONY: print
print:
	$(foreach v, $(V), \
		$(warning $v = $($v)))


