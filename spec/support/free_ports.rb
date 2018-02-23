
# Dispenses free TCP ports for tests
module FreePorts
  FIRST = 3030

  def self.take_next
    @next ||= FIRST
    @next += 1
    @next - 1
  end
end
